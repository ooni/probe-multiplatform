package org.ooni.probe

import android.app.Application
import android.app.LocaleConfig
import android.app.LocaleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.LocaleList
import android.os.PowerManager
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.work.WorkManager
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import okio.Path.Companion.toPath
import org.ooni.engine.AndroidNetworkTypeFinder
import org.ooni.engine.AndroidOonimkallBridge
import org.ooni.probe.background.AppWorkerManager
import org.ooni.probe.config.AndroidBatteryOptimization
import org.ooni.probe.data.models.FileSharing
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo

/**
 * See link for `baseFileDir` https://github.com/ooni/probe-android/blob/5a11d1a36ec952aa1f355ba8db4129146139a5cc/engine/src/main/java/org/openobservatory/engine/Engine.java#L52
 * See link for `cacheDir` https://github.com/ooni/probe-android/blob/5a11d1a36ec952aa1f355ba8db4129146139a5cc/engine/src/main/java/org/openobservatory/engine/Engine.java#L70
 */
class AndroidApplication : Application() {
    val dependencies by lazy {
        Dependencies(
            platformInfo = platformInfo,
            oonimkallBridge = AndroidOonimkallBridge(),
            baseFileDir = filesDir.absolutePath,
            cacheDir = cacheDir.absolutePath,
            readAssetFile = ::readAssetFile,
            databaseDriverFactory = ::buildDatabaseDriver,
            networkTypeFinder = AndroidNetworkTypeFinder(connectivityManager),
            buildDataStore = ::buildDataStore,
            isBatteryCharging = ::checkBatteryCharging,
            launchUrl = ::launchUrl,
            startSingleRunInner = appWorkerManager::startSingleRun,
            configureAutoRun = appWorkerManager::configureAutoRun,
            openVpnSettings = ::openVpnSettings,
            configureDescriptorAutoUpdate = appWorkerManager::configureDescriptorAutoUpdate,
            fetchDescriptorUpdate = appWorkerManager::fetchDescriptorUpdate,
            shareFile = ::shareFile,
            batteryOptimization = batteryOptimization,
        )
    }

    private val mainActivityLifecycleCallbacks = MainActivityLifecycleCallbacks()

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val localeManager = applicationContext
                .getSystemService(LocaleManager::class.java)
            localeManager.overrideLocaleConfig = LocaleConfig(
                LocaleList.forLanguageTags(getString(R.string.supported_languages)),
            )
        }
        registerActivityLifecycleCallbacks(mainActivityLifecycleCallbacks)
    }

    private val platformInfo by lazy {
        object : PlatformInfo {
            override val buildName: String = BuildConfig.VERSION_NAME
            override val buildNumber: String = BuildConfig.VERSION_CODE.toString()
            override val platform = Platform.Android
            override val osVersion = Build.VERSION.SDK_INT.toString()
            override val model = "${Build.MANUFACTURER} ${Build.MODEL}"
            override val needsToRequestNotificationsPermission =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        }
    }

    private fun readAssetFile(path: String) = assets.open(path).bufferedReader().use { it.readText() }

    private val connectivityManager get() = getSystemService(ConnectivityManager::class.java)

    private fun buildDatabaseDriver(): SqlDriver = AndroidSqliteDriver(Database.Schema, this, "v2.db")

    private fun buildDataStore(): DataStore<Preferences> =
        Dependencies.getDataStore(
            producePath = { this.filesDir.resolve(Dependencies.Companion.DATA_STORE_FILE_NAME).absolutePath },
            migrations = listOf(SharedPreferencesMigration(this, "${packageName}_preferences")),
        )

    private fun checkBatteryCharging(): Boolean {
        val batteryManager = this.getSystemService(BATTERY_SERVICE) as? BatteryManager
        return batteryManager?.isCharging == true
    }

    private fun launchUrl(
        url: String,
        extras: Map<String, String>?,
    ) {
        val uri = Uri.parse(url)
        val intent = Intent(
            when (uri.scheme) {
                "mailto" -> Intent.ACTION_SENDTO
                else -> Intent.ACTION_VIEW
            },
            uri,
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (uri.scheme == "mailto") {
            val chooserTitle = extras?.get("chooserTitle") ?: "Send email"
            val mailerIntent = Intent.createChooser(intent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                extras?.forEach { (key, value) ->
                    when (key) {
                        "subject" -> putExtra(Intent.EXTRA_SUBJECT, value)
                        "body" -> putExtra(Intent.EXTRA_TEXT, value)
                    }
                }
            }
            startActivity(mailerIntent)
        } else {
            startActivity(intent)
        }
    }

    /**
     * From https://developer.android.com/training/monitoring-device-state/battery-monitoring#DetermineChargeState
     *
     * Influences auto-run behavior. https://github.com/ooni/probe-android/blob/366c5cffc913362243df20b5b0477b7ea7d35b16/app/src/main/java/org/openobservatory/ooniprobe/domain/GenerateAutoRunServiceSuite.java#L35-L37
     */
    fun getChargingLevel(context: Context): Float {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(null, ifilter)?.let { batteryStatus ->
            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = level * 100 / scale.toFloat()
            return batteryPct
        } ?: run {
            return 0.0f
        }
    }

    private val appWorkerManager by lazy {
        AppWorkerManager(
            WorkManager.getInstance(this),
            Dispatchers.IO,
        )
    }

    private fun openVpnSettings() =
        try {
            startActivity(
                Intent("android.net.vpn.SETTINGS")
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        } catch (e: ActivityNotFoundException) {
            Logger.e("Could not open VPN Settings", e)
            false
        }

    private fun shareFile(fileSharing: FileSharing): Boolean {
        val file = filesDir.absolutePath.toPath().resolve(fileSharing.filePath).toFile()
        if (!file.exists()) {
            Logger.w("File to share does not exist: $file")
            return false
        }

        val uri = try {
            FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file)
        } catch (e: IllegalArgumentException) {
            Logger.w("Could not generate file uri to share", e)
            return false
        }

        return try {
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND)
                        .setType("*/*")
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                    fileSharing.title,
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        } catch (e: ActivityNotFoundException) {
            Logger.e("Could not share file", e)
            false
        }
    }

    private val batteryOptimization by lazy {
        AndroidBatteryOptimization(
            powerManager = getSystemService(PowerManager::class.java),
            packageName = packageName,
            requestCall = { callback ->
                mainActivityLifecycleCallbacks.activity?.requestIgnoreBatteryOptimization(callback)
            },
        )
    }
}
