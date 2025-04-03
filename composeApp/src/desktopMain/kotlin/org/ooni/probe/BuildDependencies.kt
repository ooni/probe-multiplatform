package org.ooni.probe

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
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
import java.io.File

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
        needsToRequestNotificationsPermission = false,
        knownBatteryState = false,
        sentryDsn = "https://e33da707dc40ab9508198b62de9bc269@o155150.ingest.sentry.io/4509084408610816",
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
    // TODO: Desktop - launchAction
    return true
}

private fun isWebViewAvailable(): Boolean {
    // TODO: Desktop - isWebViewAvailable
    return true
}

private class DesktopFlavorConfig : FlavorConfigInterface {
    override val optionalFeatures = setOf(OptionalFeature.CrashReporting)
}
