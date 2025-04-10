package org.ooni.probe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.kdroidfilter.platformtools.darkmodedetector.isSystemInDarkMode
import io.github.kdroidfilter.platformtools.darkmodedetector.windows.setWindowsAdaptiveTitleBar
import io.github.vinceglb.autolaunch.AutoLaunch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.app_name
import ooniprobe.composeapp.generated.resources.tray_icon_dark
import ooniprobe.composeapp.generated.resources.tray_icon_dark_running
import ooniprobe.composeapp.generated.resources.tray_icon_light
import ooniprobe.composeapp.generated.resources.tray_icon_light_running
import ooniprobe.composeapp.generated.resources.tray_icon_windows_dark
import ooniprobe.composeapp.generated.resources.tray_icon_windows_dark_running
import ooniprobe.composeapp.generated.resources.tray_icon_windows_light
import ooniprobe.composeapp.generated.resources.tray_icon_windows_light_running
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.DeepLinkHandler
import org.ooni.probe.data.models.DeepLink
import org.ooni.probe.data.models.RunBackgroundState
import org.ooni.probe.shared.DesktopOS
import org.ooni.probe.shared.Platform
import java.awt.Desktop

val APP_ID = "org.openobservatory.ooniprobe"

fun main(args: Array<String>) {
    val autoLaunch = AutoLaunch(appPackageName = APP_ID)

    val deepLinkFlow = MutableSharedFlow<DeepLink?>(extraBufferCapacity = 1)

    // Initialize the deep link handler
    val deepLinkHandler = DeepLinkHandler()
    deepLinkHandler.initialize(args)

    if ((dependencies.platformInfo.platform as? Platform.Desktop)?.os == DesktopOS.Mac) {
        Desktop.getDesktop().setOpenURIHandler { event ->
            deepLinkFlow.tryEmit(DeepLink.AddDescriptor(event.uri.path.split("/").last()))
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
            deepLinkHandler.addMessageListener { message ->
                message?.let { message ->
                    isWindowVisible = true
                    deepLinkFlow.tryEmit(DeepLink.AddDescriptor(message.split("/").last()))
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
            icon = painterResource(trayIcon()),
            title = stringResource(Res.string.app_name),
        ) {
            window.setWindowsAdaptiveTitleBar()
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
            icon = painterResource(trayIcon()),
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
                        deepLinkHandler.shutdown()
                        exitApplication()
                    },
                )
            },
        )
    }
}

@Composable
private fun trayIcon(): DrawableResource {
    val isDarkTheme = isSystemInDarkMode()
    val isWindows = (dependencies.platformInfo.platform as? Platform.Desktop)?.os == DesktopOS.Windows
    val runBackgroundState by dependencies.runBackgroundStateManager.observeState()
        .collectAsState(RunBackgroundState.Idle())
    val isRunning = runBackgroundState !is RunBackgroundState.Idle
    return when {
        isDarkTheme && isWindows && isRunning -> Res.drawable.tray_icon_windows_dark_running
        !isDarkTheme && isWindows && isRunning -> Res.drawable.tray_icon_windows_light_running
        isDarkTheme && !isWindows && isRunning -> Res.drawable.tray_icon_dark_running
        !isDarkTheme && !isWindows && isRunning -> Res.drawable.tray_icon_light_running
        isDarkTheme && isWindows && !isRunning -> Res.drawable.tray_icon_windows_dark
        !isDarkTheme && isWindows && !isRunning -> Res.drawable.tray_icon_windows_light
        isDarkTheme && !isWindows && !isRunning -> Res.drawable.tray_icon_dark
        else -> Res.drawable.tray_icon_light
    }
}

private fun startSingleRun() {
    CoroutineScope(Dispatchers.IO).launch {
        dependencies.runBackgroundTask(null).collect()
    }
}
