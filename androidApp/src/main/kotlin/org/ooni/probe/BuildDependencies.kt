package org.ooni.probe

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.text.TextUtils
import android.view.View
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.webkit.WebViewCompat
import androidx.work.WorkManager
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import okio.Path
import okio.Path.Companion.toPath
import org.ooni.engine.AndroidNetworkTypeFinder
import org.ooni.engine.AndroidOonimkallBridge
import org.ooni.engine.AndroidResolverTypeFinder
import org.ooni.engine.AndroidSecureStorage
import org.ooni.engine.NetworkTypeFinder
import org.ooni.engine.ResolverTypeMapper
import org.ooni.passport.AndroidPassportBridge
import org.ooni.probe.background.AppWorkerManager
import org.ooni.probe.config.AndroidBatteryOptimization
import org.ooni.probe.config.FlavorConfig
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.config.ProxyConfig
import org.ooni.probe.data.models.BatteryState
import org.ooni.probe.data.models.PlatformAction
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.LanguageSupport
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo
import java.util.Locale

/**
 * See link for `baseFileDir` https://github.com/ooni/probe-android/blob/5a11d1a36ec952aa1f355ba8db4129146139a5cc/engine/src/main/java/org/openobservatory/engine/Engine.java#L52
 * See link for `cacheDir` https://github.com/ooni/probe-android/blob/5a11d1a36ec952aa1f355ba8db4129146139a5cc/engine/src/main/java/org/openobservatory/engine/Engine.java#L70
 */
internal fun buildDependencies(
    app: AndroidApplication,
    mainActivity: () -> MainActivity?,
): Dependencies {
    val connectivityManager = app.getSystemService(ConnectivityManager::class.java)
    val appWorkerManager = appWorkerManager(app)
    return Dependencies(
        platformInfo = platformInfo(app),
        oonimkallBridge = AndroidOonimkallBridge(),
        passportBridge = AndroidPassportBridge(),
        baseFileDir = app.filesDir.absolutePath,
        cacheDir = app.cacheDir.absolutePath,
        databaseDriverFactory = { buildDatabaseDriver(app) },
        networkTypeFinder = AndroidNetworkTypeFinder(connectivityManager),
        buildResolverTypeFinder = { nf, rtf ->
            buildResolverTypeFinder(connectivityManager, nf, rtf)
        },
        secureStorage = AndroidSecureStorage(app, OrganizationConfig.baseSoftwareName),
        buildDataStore = { buildDataStore(app) },
        getBatteryState = { getBatteryState(app) },
        startSingleRunInner = appWorkerManager::startSingleRun,
        configureAutoRun = appWorkerManager::configureAutoRun,
        configureDescriptorAutoUpdate = appWorkerManager::configureDescriptorAutoUpdate,
        cancelDescriptorAutoUpdate = appWorkerManager::cancelDescriptorAutoUpdate,
        startDescriptorsUpdate = appWorkerManager::startDescriptorsUpdate,
        launchAction = { action -> launchAction(app, action) },
        batteryOptimization = batteryOptimization(app, mainActivity),
        isWebViewAvailable = { isWebViewAvailable(app) },
        flavorConfig = FlavorConfig(),
        proxyConfig = ProxyConfig(isPsiphonSupported = false),
        getCountryNameByCode = ::getCountryNameByCode,
        getLanguageNameByCode = ::getLanguageNameByCode,
        localeDirection = ::localeDirection,
    )
}

private fun platformInfo(app: Application): PlatformInfo {
    val packageInfo = app.packageManager.getPackageInfo(app.packageName, 0)
    return PlatformInfo(
        buildName = packageInfo.versionName.orEmpty(),
        buildNumber = PackageInfoCompat.getLongVersionCode(packageInfo).toString(),
        platform = Platform.Android,
        osVersion = Build.VERSION.SDK_INT.toString(),
        model = "${Build.MANUFACTURER} ${Build.MODEL}",
        requestNotificationsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
        languageSupport = if (Build.VERSION.SDK_INT < 33) {
            LanguageSupport.IN_APP
        } else {
            LanguageSupport.SYSTEM_SETTINGS
        },
        hasDonations = true,
        canPullToRefresh = true,
        sentryDsn =
            "https://7a49ffedcb48b9b69705d1ac2c032c69@o155150.ingest.sentry.io/4508325642764288",
        sentryExtraTags = mapOf(
            // `xperimental` reports to the same project as `full`, without ProGuard mappings
            "build_flavor" to BuildConfig.FLAVOR,
            "environment" to if (BuildConfig.DEBUG) "development" else "production",
        ),
    )
}

private fun buildDatabaseDriver(app: Application): SqlDriver =
    AndroidSqliteDriver(Database.Schema, app, "v2.db")

private fun buildDataStore(app: Application): DataStore<Preferences> =
    Dependencies.getDataStore(
        producePath = {
            app.filesDir.resolve(Dependencies.DATA_STORE_FILE_NAME).absolutePath
        },
        migrations = listOf(
            SharedPreferencesMigration(app, "${app.packageName}_preferences"),
        ),
    )

private fun buildResolverTypeFinder(
    connectivityManager: ConnectivityManager,
    networkTypeFinder: NetworkTypeFinder,
    resolverTypeMapper: ResolverTypeMapper
): AndroidResolverTypeFinder {
    return AndroidResolverTypeFinder(
        connectivityManager = connectivityManager,
        networkTypeFinder = networkTypeFinder
    )
}

private fun getBatteryState(app: Application): BatteryState {
    // From https://developer.android.com/training/monitoring-device-state/battery-monitoring#DetermineChargeState
    val batteryStatus = app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    return when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING,
        BatteryManager.BATTERY_STATUS_FULL,
            -> BatteryState.Charging

        BatteryManager.BATTERY_STATUS_NOT_CHARGING,
        BatteryManager.BATTERY_STATUS_DISCHARGING,
            -> BatteryState.NotCharging

        else -> BatteryState.Unknown
    }
}

private fun launchAction(app: Application, action: PlatformAction): Boolean =
    when (action) {
        is PlatformAction.Mail -> sendMail(app, action)
        is PlatformAction.OpenUrl -> openUrl(app, action)
        is PlatformAction.Share -> shareText(app, action)
        is PlatformAction.FileSharing -> shareFile(app, action)
        is PlatformAction.VpnSettings -> openVpnSettings(app)
        is PlatformAction.LanguageSettings -> openLanguageSettings(app)
    }

private fun sendMail(app: Application, mail: PlatformAction.Mail): Boolean {
    val attachmentUri = mail.attachment?.let { getUriForFile(app, it) }

    val intent = Intent
        .createChooser(
            Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
                .putExtra(Intent.EXTRA_EMAIL, arrayOf(mail.to))
                .putExtra(Intent.EXTRA_SUBJECT, mail.subject)
                .putExtra(Intent.EXTRA_TEXT, mail.body)
                .run {
                    attachmentUri?.let {
                        putExtra(Intent.EXTRA_STREAM, attachmentUri)
                            // Avoid SecurityException
                            .also { it.clipData = ClipData.newRawUri("", attachmentUri) }
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } ?: this
                },
            mail.chooserTitle,
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    return try {
        app.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Logger.e("Could not send mail", e)
        false
    }
}

private fun openUrl(app: Application, openUrl: PlatformAction.OpenUrl): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, openUrl.url.toUri()).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        app.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Logger.e("Could not open url", e)
        false
    }
}

private fun openVpnSettings(app: Application) =
    try {
        app.startActivity(
            Intent("android.net.vpn.SETTINGS")
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    } catch (e: ActivityNotFoundException) {
        Logger.e("Could not open VPN Settings", e)
        false
    }

private fun openLanguageSettings(app: Application) =
    try {
        app.startActivity(
            Intent(
                "android.settings.APP_LOCALE_SETTINGS",
                Uri.fromParts("package", app.packageName, null),
            ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    } catch (e: ActivityNotFoundException) {
        Logger.e("Could not open Language Settings", e)
        false
    }

private fun shareFile(app: Application, fileSharing: PlatformAction.FileSharing): Boolean {
    val uri = getUriForFile(app, fileSharing.filePath) ?: return false

    return try {
        app.startActivity(
            Intent
                .createChooser(
                    Intent(Intent.ACTION_SEND)
                        .setType("*/*")
                        // Avoid SecurityException
                        .also { it.clipData = ClipData.newRawUri("", uri) }
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

private fun getUriForFile(app: Application, filePath: Path): Uri? {
    val file = app.filesDir.absolutePath
        .toPath()
        .resolve(filePath)
        .toFile()
    if (!file.exists()) {
        Logger.w("File to share does not exist: $file")
        return null
    }

    return try {
        FileProvider.getUriForFile(app, "${app.packageName}.provider", file)
    } catch (e: IllegalArgumentException) {
        Logger.w("Could not generate file uri to share", e)
        null
    }
}

private fun shareText(app: Application, share: PlatformAction.Share): Boolean =
    try {
        app.startActivity(
            Intent
                .createChooser(
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

private fun appWorkerManager(app: Application) =
    AppWorkerManager(
        WorkManager.getInstance(app),
        Dispatchers.IO,
    )

private fun isWebViewAvailable(app: Application) =
    WebViewCompat.getCurrentWebViewPackage(app) != null

private fun batteryOptimization(app: AndroidApplication, mainActivity: () -> MainActivity?) =
    AndroidBatteryOptimization(
        powerManager = app.getSystemService(PowerManager::class.java),
        packageName = app.packageName,
        requestCall = { callback -> mainActivity()?.requestIgnoreBatteryOptimization(callback) },
    )

private fun getCountryNameByCode(countryCode: String) =
    Locale
        .Builder()
        .setRegion(countryCode)
        .build()
        .displayCountry
        .ifEmpty { countryCode }

private fun getLanguageNameByCode(languageCode: String): String {
    val locale = Locale.forLanguageTag(languageCode)
    return locale.getDisplayLanguage(locale) +
        (locale.getDisplayCountry(locale).ifEmpty { null }?.let { " ($it)" } ?: "")
}

private fun localeDirection(): LayoutDirection =
    if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
