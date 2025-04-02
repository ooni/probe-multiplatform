package org.ooni.probe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import co.touchlab.kermit.Logger
import io.github.vinceglb.autolaunch.AutoLaunch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.app_name
import ooniprobe.composeapp.generated.resources.tray_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.ui.shared.webview.WebViewSetup
import kotlin.time.Duration.Companion.hours

fun main() {
    val autoLaunch = AutoLaunch(appPackageName = "org.openobservatory.ooniprobe")
    val webViewSetup = WebViewSetup(dependencies.baseFileDir, dependencies.cacheDir)

    CoroutineScope(Dispatchers.IO).launch {
        autoLaunch.enable()
    }

    // start an hourly background task that calls startSingleRun
    CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            delay(1.hours)
            startSingleRun()
        }
    }

    CoroutineScope(Dispatchers.IO).launch {
        webViewSetup.initialize()
    }

    CoroutineScope(Dispatchers.IO).launch {
        webViewSetup.state.collectLatest { Logger.i("WebView: $it") }
    }

    application {
        var isWindowVisible by remember { mutableStateOf(!autoLaunch.isStartedViaAutostart()) }

        Window(
            onCloseRequest = {
                isWindowVisible = false
            },
            visible = isWindowVisible,
            icon = painterResource(Res.drawable.tray_icon),
            title = stringResource(Res.string.app_name),
        ) {
            App(
                dependencies = dependencies,
                deepLink = null,
                onDeeplinkHandled = {},
            )
        }

        Tray(
            icon = painterResource(Res.drawable.tray_icon),
            tooltip = stringResource(Res.string.app_name),
            menu = {
                Item(
                    "Show App",
                    onClick = {
                        isWindowVisible = !isWindowVisible
                    },
                )
                Item(
                    "Run Test",
                    onClick = ::startSingleRun,
                )
                Separator()
                Item(
                    "Exit",
                    onClick = ::exitApplication,
                )
            },
        )
    }
}

private fun startSingleRun() {
    CoroutineScope(Dispatchers.IO).launch {
        dependencies.runBackgroundTask(null).collect()
    }
}
