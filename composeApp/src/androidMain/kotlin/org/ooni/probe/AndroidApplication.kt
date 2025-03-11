package org.ooni.probe

import android.app.Application
import android.app.LocaleConfig
import android.app.LocaleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
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
import org.ooni.probe.config.FlavorConfig
import org.ooni.probe.data.models.PlatformAction
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo
import androidx.core.net.toUri

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
            startSingleRunInner = appWorkerManager::startSingleRun,
            configureAutoRun = appWorkerManager::configureAutoRun,
            configureDescriptorAutoUpdate = appWorkerManager::configureDescriptorAutoUpdate,
            startDescriptorsUpdate = appWorkerManager::startDescriptorsUpdate,
            launchAction = ::launchAction,
            batteryOptimization = batteryOptimization,
            flavorConfig = FlavorConfig(),
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
            override val sentryDsn =
                "https://7a49ffedcb48b9b69705d1ac2c032c69@o155150.ingest.sentry.io/4508325642764288"
        }
    }

    private fun readAssetFile(path: String) = assets.open(path).bufferedReader().use { it.readText() }

    private val connectivityManager get() = getSystemService(ConnectivityManager::class.java)

    private fun buildDatabaseDriver(): SqlDriver = AndroidSqliteDriver(Database.Schema, this, "v2.db")

    private fun buildDataStore(): DataStore<Preferences> =
        Dependencies.getDataStore(
            producePath = {
                filesDir.resolve(Dependencies.Companion.DATA_STORE_FILE_NAME).absolutePath
            },
            migrations = listOf(
                SharedPreferencesMigration(this, "${packageName}_preferences"),
            ),
        )

    private fun checkBatteryCharging(): Boolean {
        // From https://developer.android.com/training/monitoring-device-state/battery-monitoring#DetermineChargeState
        val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun launchAction(action: PlatformAction): Boolean {
        return when (action) {
            is PlatformAction.Mail -> sendMail(action)
            is PlatformAction.OpenUrl -> openUrl(action)
            is PlatformAction.Share -> shareText(action)
            is PlatformAction.FileSharing -> shareFile(action)
            is PlatformAction.VpnSettings -> openVpnSettings()
        }
    }

    private fun sendMail(mail: PlatformAction.Mail): Boolean {
        val intent = Intent.createChooser(
            Intent(Intent.ACTION_SENDTO, "mailto:${mail.to}".toUri()).apply {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(mail.to))
                putExtra(Intent.EXTRA_SUBJECT, mail.subject)
                putExtra(Intent.EXTRA_TEXT, mail.body)
            },
            mail.chooserTitle,
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Logger.e("Could not send mail", e)
            false
        }
    }

    private fun openUrl(openUrl: PlatformAction.OpenUrl): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, openUrl.url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Logger.e("Could not open url", e)
            false
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

    private fun shareFile(fileSharing: PlatformAction.FileSharing): Boolean {
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

    private fun shareText(share: PlatformAction.Share): Boolean {
        return try {
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, share.text),
                    null,
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
