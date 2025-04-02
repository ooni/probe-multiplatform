package org.ooni.probe

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import co.touchlab.kermit.Logger
import dev.dirs.ProjectDirectories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import org.ooni.engine.DesktopOonimkallBridge
import org.ooni.engine.models.NetworkType
import org.ooni.probe.config.BatteryOptimization
import org.ooni.probe.config.FlavorConfigInterface
import org.ooni.probe.config.OptionalFeature
import org.ooni.probe.data.buildDatabaseDriver
import org.ooni.probe.data.models.AutoRunParameters
import org.ooni.probe.data.models.BatteryState
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.PlatformAction
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val projectDirectories = ProjectDirectories.from("org", "OONI", "Probe")

val dependencies = Dependencies(
    platformInfo = buildPlatformInfo(),
    oonimkallBridge = DesktopOonimkallBridge(),
    baseFileDir = projectDirectories.dataDir.also { File(it).mkdirs() },
    cacheDir = projectDirectories.cacheDir.also { File(it).mkdirs() },
    readAssetFile = ::readAssetFile,
    databaseDriverFactory = { buildDatabaseDriver(projectDirectories.dataDir) },
    networkTypeFinder = ::networkTypeFinder,
    buildDataStore = ::buildDataStore,
    getBatteryState = { BatteryState.Unknown },
    startSingleRunInner = ::startSingleRun,
    configureAutoRun = ::configureAutoRun,
    configureDescriptorAutoUpdate = ::configureDescriptorAutoUpdate,
    startDescriptorsUpdate = ::startDescriptorsUpdate,
    launchAction = ::launchAction,
    batteryOptimization = object : BatteryOptimization {},
    isWebViewAvailable = ::isWebViewAvailable,
    flavorConfig = DesktopFlavorConfig(),
)

// TODO: Desktop - PlatformInfo
private fun buildPlatformInfo() =
    PlatformInfo(
        buildName = "1.0",
        buildNumber = "1",
        platform = Platform.Desktop,
        osVersion = "1.0",
        model = "model",
        requestNotificationsPermission = false,
        knownBatteryState = false,
        sentryDsn = "",
    )

private fun readAssetFile(path: String): String {
    // Read asset is only needed for NewsMediaScan Android and iOS, not for Desktop
    throw NotImplementedError()
}

// TODO: Desktop - buildDatabaseDriver persist in storage instead of in memory
private fun networkTypeFinder() = NetworkType.Wifi

// TODO: Desktop - Confirm appropriate path and configuration
private fun buildDataStore() =
    PreferenceDataStoreFactory.create {
        projectDirectories.dataDir.toPath().resolve("probe.preferences_pb").toFile()
    }

private fun startSingleRun(spec: RunSpecification) {
    // TODO: Desktop - background running
    CoroutineScope(Dispatchers.IO).launch {
        dependencies.runBackgroundTask(spec).collect()
    }
}

private fun configureAutoRun(params: AutoRunParameters) {
    // TODO: Desktop - background running
}

private fun configureDescriptorAutoUpdate(): Boolean {
    // TODO: Desktop - background running
    return true
}

private fun startDescriptorsUpdate(descriptors: List<InstalledTestDescriptorModel>?) {
    // TODO: Desktop - background running
    CoroutineScope(Dispatchers.IO).launch {
        dependencies.getDescriptorUpdate(descriptors.orEmpty())
    }
}

private fun launchAction(action: PlatformAction): Boolean {
    return when (action) {
        is PlatformAction.FileSharing -> shareFile(action)
        is PlatformAction.Mail -> sendMail(action)
        is PlatformAction.OpenUrl -> openUrl(action)
        is PlatformAction.Share -> shareText(action)
        PlatformAction.VpnSettings -> openVpnSettings()
    }
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

private fun isWebViewAvailable(): Boolean {
    // TODO: Desktop - isWebViewAvailable
    return true
}

private class DesktopFlavorConfig : FlavorConfigInterface {
    override val optionalFeatures = setOf(OptionalFeature.CrashReporting)
}
