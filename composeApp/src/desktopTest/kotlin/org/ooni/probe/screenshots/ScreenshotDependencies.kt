package org.ooni.probe.screenshots

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.Dispatchers
import org.ooni.engine.NetworkTypeFinder
import org.ooni.engine.TestOonimkallBridge
import org.ooni.engine.createDesktopSecureStorage
import org.ooni.engine.models.NetworkType
import org.ooni.probe.config.BatteryOptimization
import org.ooni.probe.config.FlavorConfigInterface
import org.ooni.probe.config.OptionalFeature
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.config.ProxyConfig
import org.ooni.probe.data.buildDatabaseDriver
import org.ooni.probe.data.models.BatteryState
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.Distribution
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

internal fun buildScreenshotDependencies(workingDir: Path): Dependencies {
    val baseFileDir = workingDir.toAbsolutePath().toString()
    val cacheDir = Files
        .createDirectories(workingDir.resolve("cache"))
        .toAbsolutePath()
        .toString()
    val osName = System.getProperty("os.name") ?: "Mac"
    val platform = Platform.Desktop(osName)

    return Dependencies(
        platformInfo = buildScreenshotPlatformInfo(platform, osName),
        oonimkallBridge = TestOonimkallBridge(),
        baseFileDir = baseFileDir,
        cacheDir = cacheDir,
        databaseDriverFactory = { buildDatabaseDriver(baseFileDir) },
        networkTypeFinder = NetworkTypeFinder { NetworkType.Wifi },
        secureStorage = createDesktopSecureStorage(
            desktopOS = platform.os,
            appId = "${OrganizationConfig.appId}.screenshots",
            baseSoftwareName = OrganizationConfig.baseSoftwareName,
        ),
        buildDataStore = { buildScreenshotDataStore(workingDir) },
        getBatteryState = { BatteryState.NotCharging },
        startSingleRunInner = { },
        configureAutoRun = { },
        configureDescriptorAutoUpdate = { false },
        cancelDescriptorAutoUpdate = { false },
        startDescriptorsUpdate = { },
        isWebViewAvailable = { false },
        launchAction = { false },
        batteryOptimization = object : BatteryOptimization {},
        flavorConfig = ScreenshotFlavorConfig,
        proxyConfig = ProxyConfig(isPsiphonSupported = false),
        getCountryNameByCode = ::countryNameByCode,
        databaseContext = Dispatchers.IO.limitedParallelism(1),
    )
}

private object ScreenshotFlavorConfig : FlavorConfigInterface {
    // Mirror the production direct-distribution build so Privacy / crash-reporting
    // settings render the same way the Android screenshots show them.
    override val optionalFeatures: Set<OptionalFeature> = setOf(OptionalFeature.CrashReporting)
}

private fun buildScreenshotDataStore(workingDir: Path): DataStore<Preferences> {
    val file = workingDir.resolve("probe.preferences_pb").toFile()
    return PreferenceDataStoreFactory.create(produceFile = { file })
}

private fun buildScreenshotPlatformInfo(
    platform: Platform.Desktop,
    osName: String,
): PlatformInfo {
    val osVersion = System.getProperty("os.version") ?: ""
    return PlatformInfo(
        buildName = "screenshots",
        buildNumber = "0",
        platform = platform,
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

private fun countryNameByCode(code: String): String =
    Locale
        .Builder()
        .setRegion(code)
        .build()
        .displayCountry
        .ifEmpty { code }
