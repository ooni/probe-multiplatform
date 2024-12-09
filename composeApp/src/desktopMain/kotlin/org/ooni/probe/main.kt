package org.ooni.probe

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import org.ooni.engine.DesktopOonimkallBridge
import org.ooni.engine.models.NetworkType
import org.ooni.probe.config.BatteryOptimization
import org.ooni.probe.config.FlavorConfigInterface
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo

private val platformInfo by lazy {
    object : PlatformInfo {
        override val buildName: String = "1.0"
        override val buildNumber: String = "1"
        override val platform = Platform.Android
        override val osVersion = "1.0"
        override val model = "model"
        override val needsToRequestNotificationsPermission = false
        override val sentryDsn = "https://7a49ffedcb48b9b69705d1ac2c032c69@o155150.ingest.sentry.io/4508325642764288"
    }
}

private val dependencies = Dependencies(
    platformInfo = platformInfo,
    oonimkallBridge = DesktopOonimkallBridge(),
    baseFileDir = ".",
    cacheDir = ".",
    readAssetFile = { "" },
    databaseDriverFactory = {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        driver
    },
    networkTypeFinder = { NetworkType.Wifi },
    buildDataStore = {
        PreferenceDataStoreFactory.create { "probe.preferences_pb".toPath().toFile() }
    },
    isBatteryCharging = { true },
    startSingleRunInner = ::startSingleRun,
    configureAutoRun = { },
    configureDescriptorAutoUpdate = { true },
    startDescriptorsUpdate = { },
    launchAction = { true },
    batteryOptimization = object : BatteryOptimization { },
    flavorConfig = object : FlavorConfigInterface {},
)

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "GoDesktop",
        ) {
            App(
                dependencies = dependencies,
                deepLink = null,
                onDeeplinkHandled = {},
            )
        }
    }
}

private fun startSingleRun(spec: RunSpecification) {
    CoroutineScope(Dispatchers.IO).launch {
        dependencies.runBackgroundTask(spec).collect()
    }
}
