package org.ooni.probe

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import co.touchlab.kermit.Logger
import dev.dirs.ProjectDirectories
import dev.hydraulic.conveyor.control.SoftwareUpdateController
import okio.Path.Companion.toPath
import org.ooni.engine.DesktopOonimkallBridge
import org.ooni.engine.DesktopNetworkTypeFinder
import org.ooni.probe.background.BackgroundWorkManager
import org.ooni.probe.config.BatteryOptimization
import org.ooni.probe.config.FlavorConfigInterface
import org.ooni.probe.config.OptionalFeature
import org.ooni.probe.data.buildDatabaseDriver
import org.ooni.probe.data.models.BatteryState
import org.ooni.probe.data.models.PlatformAction
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val projectDirectories = ProjectDirectories.from("org", "OONI", "Probe")

private val backgroundWorkManager: BackgroundWorkManager = BackgroundWorkManager(
    runBackgroundTaskProvider = { dependencies.runBackgroundTask },
    getDescriptorUpdateProvider = { dependencies.getDescriptorUpdate },
)

val dependencies = Dependencies(
    platformInfo = buildPlatformInfo(),
    oonimkallBridge = DesktopOonimkallBridge(),
    baseFileDir = projectDirectories.dataDir.also { File(it).mkdirs() },
    cacheDir = projectDirectories.cacheDir.also { File(it).mkdirs() },
    readAssetFile = ::readAssetFile,
    databaseDriverFactory = { buildDatabaseDriver(projectDirectories.dataDir) },
    networkTypeFinder = DesktopNetworkTypeFinder(),
    buildDataStore = ::buildDataStore,
    getBatteryState = { BatteryState.Unknown },
    startSingleRunInner = backgroundWorkManager::startSingleRun,
    configureAutoRun = backgroundWorkManager::configureAutoRun,
    configureDescriptorAutoUpdate = backgroundWorkManager::configureDescriptorAutoUpdate,
    cancelDescriptorAutoUpdate = backgroundWorkManager::cancelDescriptorAutoUpdate,
    startDescriptorsUpdate = backgroundWorkManager::startDescriptorsUpdate,
    launchAction = ::launchAction,
    batteryOptimization = object : BatteryOptimization {},
    isWebViewAvailable = { true },
    flavorConfig = DesktopFlavorConfig(),
)

private fun buildPlatformInfo(): PlatformInfo {
    val osName = System.getProperty("os.name")
    val osVersion = System.getProperty("os.version")
    val conveyorVersion = SoftwareUpdateController.getInstance()?.currentVersion
    val buildName = conveyorVersion?.version
        ?: System.getProperty("jpackage.app-version")?.ifBlank { null }
        ?: "test"
    val buildNumber = conveyorVersion?.revision?.toString() ?: "0"

    return PlatformInfo(
        buildName = buildName,
        buildNumber = buildNumber,
        platform = Platform.Desktop(osName),
        osVersion = "$osName $osVersion",
        model = "",
        requestNotificationsPermission = false,
        knownBatteryState = false,
        knownNetworkType = false,
        supportsInAppLanguage = false,
        canPullToRefresh = false,
        sentryDsn = "https://e33da707dc40ab9508198b62de9bc269@o155150.ingest.sentry.io/4509084408610816",
    )
}

private fun readAssetFile(path: String): String {
    // Read asset is only needed for NewsMediaScan Android and iOS, not for Desktop
    throw NotImplementedError()
}

private fun buildDataStore() =
    PreferenceDataStoreFactory.create {
        projectDirectories.dataDir.toPath().resolve("probe.preferences_pb").toFile()
    }

private fun launchAction(action: PlatformAction): Boolean =
    when (action) {
        is PlatformAction.FileSharing -> shareFile(action)
        is PlatformAction.Mail -> sendMail(action)
        is PlatformAction.OpenUrl -> openUrl(action)
        is PlatformAction.Share -> shareText(action)
        PlatformAction.VpnSettings -> openVpnSettings()
        PlatformAction.LanguageSettings -> false
    }

fun openVpnSettings(): Boolean {
    return false
}

fun shareText(action: PlatformAction.Share): Boolean {
    return try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
            val uri =
                URI.create("mailto:?body=${URLEncoder.encode(action.text, StandardCharsets.UTF_8)}")
            Desktop.getDesktop().mail(uri)
            true
        } else {
            false
        }
    } catch (e: Exception) {
        Logger.e(e) { "Failed to share text" }
        false
    }
}

fun shareFile(action: PlatformAction.FileSharing): Boolean {
    return try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                .isSupported(Desktop.Action.APP_OPEN_FILE)
        ) {
            val file = projectDirectories.dataDir.toPath().resolve(action.filePath).toFile()
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

fun sendMail(action: PlatformAction.Mail): Boolean {
    return try {
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
}

private fun buildMailUri(action: PlatformAction.Mail): URI {
    val subject = URLEncoder.encode(action.subject, StandardCharsets.UTF_8).replace("+", "%20")
    val body = URLEncoder.encode(action.body, StandardCharsets.UTF_8).replace("+", "%20")
        .replace("%0A", "%0D%0A")
    return URI("mailto:${action.to}?subject=$subject&body=$body")
}

private class DesktopFlavorConfig : FlavorConfigInterface {
    override val optionalFeatures = setOf(OptionalFeature.CrashReporting)
}
