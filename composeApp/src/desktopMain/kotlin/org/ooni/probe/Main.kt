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
import org.ooni.probe.data.DeepLinkHandler
import org.ooni.probe.data.models.DeepLink
import org.ooni.probe.shared.DesktopOS
import org.ooni.probe.shared.Platform
import java.awt.Desktop

val APP_ID = "org.openobservatory.ooniprobe"

fun main(args: Array<String>) {
    val autoLaunch = AutoLaunch(appPackageName = APP_ID)

    val deepLinkFlow = MutableSharedFlow<DeepLink?>(extraBufferCapacity = 1)

    var deepLinkHandler: DeepLinkHandler? = null

    dependencies.platformInfo.platform.let { platform ->
        if (platform is Platform.Desktop && platform.os == DesktopOS.Mac) {
            Desktop.getDesktop().setOpenURIHandler { event ->
                deepLinkFlow.tryEmit(DeepLink.AddDescriptor(event.uri.path.split("/").last()))
            }
        } else {
            deepLinkHandler = DeepLinkHandler()
            deepLinkHandler.initialize(args)
        }
    }

    application {
        var isWindowVisible by remember { mutableStateOf(!autoLaunch.isStartedViaAutostart()) }

        val deepLink by deepLinkFlow.collectAsState(null)

        // start an hourly background task that calls startSingleRun
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000 * 60 * 60)
                startSingleRun()
            }
        }

        LaunchedEffect(Unit) {
            deepLinkHandler?.addMessageListener { message ->
                message?.let { message ->
                    isWindowVisible = true
                    deepLinkFlow.tryEmit(message)
                }
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
                    onClick = {
                        deepLinkHandler?.shutdown()
                        exitApplication()
                    },
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
