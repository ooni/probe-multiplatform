package org.ooni.probe.screenshots

import org.ooni.engine.NetworkTypeFinder
import org.ooni.engine.TestOonimkallBridge
import org.ooni.engine.models.NetworkType
import org.ooni.probe.buildDependencies
import org.ooni.probe.config.LegacyDirectoryManager
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.BatteryState
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.Distribution
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo
import java.nio.file.Files
import java.nio.file.Path

internal fun buildScreenshotDependencies(workingDir: Path): Dependencies {
    val baseFileDir = workingDir.toAbsolutePath().toString()
    val cacheDir = Files
        .createDirectories(workingDir.resolve("cache"))
        .toAbsolutePath()
        .toString()
    val osName = System.getProperty("os.name") ?: "Mac"

    return buildDependencies(
        dataDir = baseFileDir,
        cacheDir = cacheDir,
        platformInfo = screenshotPlatformInfo(osName),
        oonimkallBridge = TestOonimkallBridge(),
        networkTypeFinder = NetworkTypeFinder { NetworkType.Wifi },
        secureStorageAppId = "${OrganizationConfig.appId}.screenshots",
        dataStoreFile = workingDir.resolve("probe.preferences_pb").toFile(),
        batteryState = BatteryState.NotCharging,
        backgroundWorkManager = null,
        // Screenshot tests render a Compose-native facsimile of the explorer page in place of
        // the JavaFX WebView (see ScreenshotOoniWebView.kt). Flipping this true lets
        // MeasurementViewModel enter ShowMeasurement so MeasurementScreen actually composes.
        isWebViewAvailable = { true },
        launchAction = { false },
        legacyDirectoryManager = object : LegacyDirectoryManager {},
        // flavorConfig keeps the default DesktopFlavorConfig: it mirrors the production
        // direct-distribution build, so Privacy / crash-reporting render like the Android shots.
    )
}

private fun screenshotPlatformInfo(osName: String): PlatformInfo {
    val osVersion = System.getProperty("os.version") ?: ""
    return PlatformInfo(
        buildName = "screenshots",
        buildNumber = "0",
        platform = Platform.Desktop(osName),
        osVersion = "$osName $osVersion".trim(),
        model = "",
        requestNotificationsPermission = false,
        knownBatteryState = false,
        knownNetworkType = false,
        supportsInAppLanguage = false,
        hasDonations = true,
        canPullToRefresh = false,
        sentryDsn = "",
        sentryExtraTags = emptyMap(),
        installerStore = Distribution.current.name,
    )
}
