package org.ooni.probe

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.app_name
import ooniprobe.composeapp.generated.resources.tray_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.DeepLink
import java.awt.Desktop

fun main() {
    application {
        val autoLaunch = AutoLaunch(appPackageName = "org.openobservatory.ooniprobe")

        var isWindowVisible by remember { mutableStateOf(!autoLaunch.isStartedViaAutostart()) }
        val deepLinkFlow = MutableSharedFlow<DeepLink?>(extraBufferCapacity = 1)
        val deepLink by deepLinkFlow.collectAsState(null)

        Desktop.getDesktop().setOpenURIHandler { event ->
            deepLinkFlow.tryEmit(DeepLink.AddDescriptor(event.uri.path.split("/").last()))
        }
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
            icon = painterResource(Res.drawable.tray_icon),
            title = stringResource(Res.string.app_name),
        ) {
            App(
                dependencies = dependencies,
                deepLink = deepLink,
                onDeeplinkHandled = {
                    deepLink?.let {
                        deepLinkFlow.tryEmit(null)
                    }
                },
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
