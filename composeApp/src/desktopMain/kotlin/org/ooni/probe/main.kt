package org.ooni.probe

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.vinceglb.autolaunch.AutoLaunch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.ooni_logo
import org.jetbrains.compose.resources.painterResource
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
    fetchDescriptorUpdate = { },
    launchAction = { true },
    batteryOptimization = object : BatteryOptimization { },
    flavorConfig = object : FlavorConfigInterface {},
)

fun main() {
    application {
        val autoLaunch = AutoLaunch(appPackageName = "org.openobservatory.ooniprobe")

        var isWindowVisible by remember { mutableStateOf(!autoLaunch.isStartedViaAutostart()) }

        LaunchedEffect(Unit){
            autoLaunch.enable()
        }

        // start an hourly background task that calls startSingleRun
        LaunchedEffect(Unit) {
            while (true) {
                startSingleRun()
                delay(1000 * 60 * 60)
            }
        }

        Window(
            onCloseRequest = {
                isWindowVisible = false
            },
            visible = isWindowVisible,
            title = "OONI Probe",
            icon = TrayIcon,
        ) {
            App(
                dependencies = dependencies,
                deepLink = null,
                onDeeplinkHandled = {},
            )
        }
        Tray(
            icon = painterResource(Res.drawable.ooni_logo),
            tooltip = "OONI Probe",
            menu = {
                Item(
                    "Show App",
                    onClick = {
                        isWindowVisible = !isWindowVisible
                    }
                )
                Item(
                    "Run Test",
                    onClick = ::startSingleRun
                )
                Separator()
                Item(
                    "Exit",
                    onClick = ::exitApplication
                )
            }
        )
    }
}

object TrayIcon : Painter() {
    override val intrinsicSize = Size(256f, 256f)

    override fun DrawScope.onDraw() {
        drawOval(Color(0xFF2988CC))
    }
}

private fun startSingleRun(spec: RunSpecification) {
    CoroutineScope(Dispatchers.IO).launch {
        dependencies.runBackgroundTask(spec).collect()
    }
}
private fun startSingleRun() {
    CoroutineScope(Dispatchers.IO).launch {
        dependencies.runBackgroundTask(null).collect()
    }
}
