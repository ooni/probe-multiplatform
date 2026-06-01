package org.ooni.probe

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import co.touchlab.kermit.Logger
import dev.dirs.ProjectDirectories
import io.github.vinceglb.autolaunch.AutoLaunch
import kotlinx.coroutines.asCoroutineDispatcher
import okio.Path.Companion.toPath
import org.ooni.engine.DesktopNetworkTypeFinder
import org.ooni.engine.NetworkTypeFinder
import org.ooni.engine.OonimkallBridge
import org.ooni.engine.createDesktopSecureStorage
import org.ooni.probe.config.OrganizationConfig
import org.ooni.engine.DesktopOonimkallBridge
import org.ooni.probe.background.BackgroundWorkManager
import org.ooni.probe.config.BatteryOptimization
import org.ooni.probe.config.DesktopLegacyDirectoryManager
import org.ooni.probe.config.FlavorConfigInterface
import org.ooni.probe.config.LegacyDirectoryManager
import org.ooni.probe.config.OptionalFeature
import org.ooni.probe.config.ProxyConfig
import org.ooni.probe.data.buildDatabaseDriver
import org.ooni.probe.data.models.BatteryState
import org.ooni.probe.data.models.PlatformAction
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.Distribution
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.Executors

internal val projectDirectories = ProjectDirectories.from("org", "OONI", "Probe")

// Debug builds keep their data/cache under composeApp/build/ so they never touch a production
// install and are wiped by `gradle clean`. Release builds use the OS-standard user directories.
// Everything (DataStore, database, shared files) derives from these.
internal val baseDataDir: String =
    if (DesktopBuildConfig.IS_DEBUG) File(DesktopBuildConfig.BUILD_DIR, "debug-data").path else projectDirectories.dataDir
internal val baseCacheDir: String =
    if (DesktopBuildConfig.IS_DEBUG) File(DesktopBuildConfig.BUILD_DIR, "debug-cache").path else projectDirectories.cacheDir
private val osName = System.getProperty("os.name")
val platform = Platform.Desktop(osName)

private val autoLaunch by lazy { AutoLaunch(appPackageName = APP_ID) }

// Pin all SQLite access to a single dedicated thread. SQLDelight's JdbcSqliteDriver
// uses a per-thread `ThreadLocal<Connection>`; under `Dispatchers.IO.limitedParallelism(1)`
// the underlying physical IO thread can rotate, causing the driver to open a new SQLite
// connection per thread. That triggered intermittent "Results screen does not refresh"
// reports on Desktop where new test results were present in the DB but the Flow-backed UI
// kept showing a stale snapshot until app restart.
private val databaseDispatcher by lazy {
    Executors
        .newSingleThreadExecutor { runnable ->
            Thread(runnable, "ooni-database").apply { isDaemon = true }
        }.asCoroutineDispatcher()
}

/**
 * Registers or deregisters the OS "run at startup" login item, backed by the
 * cross-platform AutoLaunch library (macOS LaunchAgent, Windows Run key, Linux
 * autostart entry).
 */
internal suspend fun setRunAtStartup(enabled: Boolean) {
    if (enabled) autoLaunch.enable() else autoLaunch.disable()
}

private val backgroundWorkManager: BackgroundWorkManager = BackgroundWorkManager(
    runBackgroundTaskProvider = { dependencies.runBackgroundTask },
    getDescriptorUpdateProvider = { dependencies.fetchDescriptorsUpdates },
)

val dependencies = buildDependencies(backgroundWorkManager = backgroundWorkManager)

/**
 * Builds the [Dependencies] graph for the desktop app. Defaults match the production flavor; callers
 * (e.g. UI/screenshot tests) override only the pieces that need to differ.
 */
internal fun buildDependencies(
    dataDir: String = baseDataDir.also { File(it).mkdirs() },
    cacheDir: String = baseCacheDir.also { File(it).mkdirs() },
    platformInfo: PlatformInfo = buildPlatformInfo(),
    oonimkallBridge: OonimkallBridge = DesktopOonimkallBridge(),
    networkTypeFinder: NetworkTypeFinder = DesktopNetworkTypeFinder(),
    secureStorageAppId: String = OrganizationConfig.appId,
    dataStoreFile: File = File(dataDir).resolve("probe.preferences_pb"),
    batteryState: BatteryState = BatteryState.Unknown,
    backgroundWorkManager: BackgroundWorkManager? = null,
    isWebViewAvailable: () -> Boolean = { true },
    launchAction: (PlatformAction) -> Boolean = ::launchAction,
    legacyDirectoryManager: LegacyDirectoryManager = DesktopLegacyDirectoryManager(platform.os),
    flavorConfig: FlavorConfigInterface = DesktopFlavorConfig(),
    setRunAtStartup: suspend (Boolean) -> Unit = ::setRunAtStartup,
): Dependencies =
    Dependencies(
        platformInfo = platformInfo,
        oonimkallBridge = oonimkallBridge,
        baseFileDir = dataDir,
        cacheDir = cacheDir,
        databaseDriverFactory = { buildDatabaseDriver(dataDir) },
        networkTypeFinder = networkTypeFinder,
        secureStorage = createDesktopSecureStorage(platform.os, secureStorageAppId, OrganizationConfig.baseSoftwareName),
        buildDataStore = { PreferenceDataStoreFactory.create { dataStoreFile } },
        getBatteryState = { batteryState },
        startSingleRunInner = { backgroundWorkManager?.startSingleRun(it) },
        configureAutoRun = { backgroundWorkManager?.configureAutoRun(it) },
        configureDescriptorAutoUpdate = { backgroundWorkManager?.configureDescriptorAutoUpdate() ?: false },
        cancelDescriptorAutoUpdate = { backgroundWorkManager?.cancelDescriptorAutoUpdate() ?: false },
        startDescriptorsUpdate = { backgroundWorkManager?.startDescriptorsUpdate(it) },
        setRunAtStartup = setRunAtStartup,
        launchAction = launchAction,
        batteryOptimization = object : BatteryOptimization {},
        isWebViewAvailable = isWebViewAvailable,
        legacyDirectoryManager = legacyDirectoryManager,
        flavorConfig = flavorConfig,
        proxyConfig = ProxyConfig(isPsiphonSupported = false),
        getCountryNameByCode = ::getCountryNameByCode,
        databaseContext = databaseDispatcher,
    )

internal fun buildPlatformInfo(): PlatformInfo {
    val osVersion = System.getProperty("os.version")
    val buildName = DesktopBuildConfig.VERSION_NAME
    val buildNumber = DesktopBuildConfig.VERSION_CODE.toString()
    val isDebug = DesktopBuildConfig.IS_DEBUG
    val environment = if (isDebug) "development" else "production"

    return PlatformInfo(
        buildName = buildName,
        buildNumber = buildNumber,
        platform = platform,
        osVersion = "$osName $osVersion",
        model = "",
        requestNotificationsPermission = false,
        knownBatteryState = false,
        knownNetworkType = false,
        supportsInAppLanguage = false,
        hasDonations = true,
        canPullToRefresh = false,
        supportsRunAtStartup = true,
        sentryDsn = "https://e33da707dc40ab9508198b62de9bc269@o155150.ingest.sentry.io/4509084408610816",
        sentryExtraTags = buildMap {
            put("os.name", osName)
            put("os.version", osVersion)
            put("os.arch", System.getProperty("os.arch"))
            put("locale", Locale.getDefault().toString())
            put("environment", environment)
            put("installerStore", Distribution.current.name)
        },
        installerStore = Distribution.current.name,
    )
}

private fun formatBytes(bytes: Long): String {
    val gib = bytes.toDouble() / (1024 * 1024 * 1024)
    return "%.1f GiB".format(gib)
}

internal fun launchAction(action: PlatformAction): Boolean =
    when (action) {
        is PlatformAction.FileSharing -> shareFile(action)
        is PlatformAction.Mail -> sendMail(action)
        is PlatformAction.OpenUrl -> openUrl(action)
        is PlatformAction.Share -> false
        PlatformAction.VpnSettings -> openVpnSettings()
        PlatformAction.LanguageSettings -> false
    }

fun openVpnSettings(): Boolean = false

fun shareFile(action: PlatformAction.FileSharing): Boolean {
    return try {
        if (Desktop.isDesktopSupported() &&
            Desktop
                .getDesktop()
                .isSupported(Desktop.Action.APP_OPEN_FILE)
        ) {
            val file = baseDataDir
                .toPath()
                .resolve(action.filePath)
                .toFile()
            if (!file.exists()) {
                Logger.w("File to share does not exist: $file")
                return false
            }
            Desktop.getDesktop().open(file)
            true
        } else {
            false
        }
    } catch (e: Exception) {
        Logger.e(e) { "Failed to share file" }
        false
    }
}

fun openUrl(action: PlatformAction.OpenUrl): Boolean {
    if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(URI(action.url))
        return true
    } else {
        return false
    }
}

fun sendMail(action: PlatformAction.Mail): Boolean =
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
            val mailUri = buildMailUri(action)
            Desktop.getDesktop().mail(mailUri)
            true
        } else {
            false
        }
    } catch (e: Exception) {
        Logger.e(e) { "Failed to send mail" }
        false
    }

private fun getCountryNameByCode(countryCode: String) =
    Locale
        .Builder()
        .setRegion(countryCode)
        .build()
        .displayCountry
        .ifEmpty { countryCode }

private fun buildMailUri(action: PlatformAction.Mail): URI {
    val subject = URLEncoder.encode(action.subject, StandardCharsets.UTF_8).replace("+", "%20")
    val body = URLEncoder
        .encode(action.body, StandardCharsets.UTF_8)
        .replace("+", "%20")
        .replace("%0A", "%0D%0A")
    return URI("mailto:${action.to}?subject=$subject&body=$body")
}

internal class DesktopFlavorConfig : FlavorConfigInterface {
    override val optionalFeatures = setOf(OptionalFeature.CrashReporting)
}
