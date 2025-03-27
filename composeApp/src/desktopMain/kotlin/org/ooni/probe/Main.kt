package org.ooni.probe

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.vinceglb.autolaunch.AutoLaunch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.app_name
import ooniprobe.composeapp.generated.resources.ooni_bw
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

fun main() {
    application {
        val autoLaunch = AutoLaunch(appPackageName = "org.openobservatory.ooniprobe")

        var isWindowVisible by remember { mutableStateOf(!autoLaunch.isStartedViaAutostart()) }

        // start an hourly background task that calls startSingleRun
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000 * 60 * 60)
                startSingleRun()
            }
        }

        LaunchedEffect(Unit) {
            autoLaunch.enable()
        }

        Window(
            onCloseRequest = {
                isWindowVisible = false
            },
            visible = isWindowVisible,
            icon = painterResource(Res.drawable.ooni_bw),
            title = stringResource(Res.string.app_name),
        ) {
            App(
                dependencies = dependencies,
                deepLink = null,
                onDeeplinkHandled = {},
            )
        }

        Tray(
            icon = painterResource(Res.drawable.ooni_bw),
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
