package org.ooni.probe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.kdroidfilter.platformtools.darkmodedetector.isSystemInDarkMode
import io.github.kdroidfilter.platformtools.darkmodedetector.windows.setWindowsAdaptiveTitleBar
import io.github.vinceglb.autolaunch.AutoLaunch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Running
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Stopping_Title
import ooniprobe.composeapp.generated.resources.Desktop_OpenApp
import ooniprobe.composeapp.generated.resources.Desktop_Quit
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Results_UploadingMissing
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
import org.ooni.probe.data.models.DeepLink
import org.ooni.probe.data.models.RunBackgroundState
import org.ooni.probe.domain.UploadMissingMeasurements
import org.ooni.probe.shared.DeepLinkParser
import org.ooni.probe.shared.DesktopOS
import org.ooni.probe.shared.InstanceManager
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.createUpdateManager
import java.awt.Desktop
import java.awt.Dimension

const val APP_ID = "org.ooni.probe" // needs to be the same as conveyor `app.rdns-name`
const val APPCAST_URL = "http://10.0.247.73:8000/appcast-aarch64.rss"
const val SPARKLE_PUBLIC_KEY = "pfIShU4dEXqPd5ObYNfDBiQWcXozk7estwzTnF9BamQ="

fun main(args: Array<String>) {
    val autoLaunch = AutoLaunch(appPackageName = APP_ID)
    val instanceManager = InstanceManager(dependencies.platformInfo)
    val deepLinkFlow = MutableSharedFlow<DeepLink?>(extraBufferCapacity = 1)
    val updateManager = createUpdateManager(dependencies.platformInfo.platform)

    CoroutineScope(Dispatchers.IO).launch {
        instanceManager.observeUrls().collectLatest {
            deepLinkFlow.tryEmit(DeepLinkParser(it))
        }
    }

    instanceManager.initialize(args)

    CoroutineScope(Dispatchers.Default).launch {
        autoLaunch.enable()
    }

    // Initialize update manager
    CoroutineScope(Dispatchers.Default).launch {
        updateManager.initialize(APPCAST_URL, SPARKLE_PUBLIC_KEY)
        updateManager.setAutomaticUpdatesEnabled(true)
        updateManager.setUpdateCheckInterval(24) // Check every 24 hours
    }

    application {
        var isWindowVisible by remember { mutableStateOf(!autoLaunch.isStartedViaAutostart()) }
        val trayIcon = trayIcon()
        val deepLink by deepLinkFlow.collectAsState(null)
        val runBackgroundState by dependencies.runBackgroundStateManager
            .observeState()
            .collectAsState(RunBackgroundState.Idle())

        fun showWindow() {
            isWindowVisible = true
            Desktop.getDesktop().requestForeground(true)
        }

        Window(
            onCloseRequest = { isWindowVisible = false },
            visible = isWindowVisible,
            icon = painterResource(trayIcon),
            title = stringResource(Res.string.app_name),
            state = rememberWindowState(
                size = DpSize(480.dp, 800.dp),
            ),
        ) {
            window.setWindowsAdaptiveTitleBar()
            window.minimumSize = Dimension(320, 560)
            window.maximumSize = Dimension(1024, 1024)

            App(
                dependencies = dependencies,
                deepLink = deepLink,
                onDeeplinkHandled = {
                    showWindow()
                    deepLink?.let {
                        deepLinkFlow.tryEmit(null)
                    }
                },
            )
        }

        Tray(
            icon = painterResource(trayIcon),
            tooltip = stringResource(Res.string.app_name),
            menu = {
                Item(
                    text = stringResource(Res.string.app_name),
                    enabled = false,
                    onClick = {},
                )
                if (runBackgroundState !is RunBackgroundState.Idle) {
                    Item(
                        text = runBackgroundState.text(),
                        enabled = false,
                        onClick = {},
                    )
                }
                Separator()
                Item(
                    stringResource(Res.string.Desktop_OpenApp),
                    onClick = { showWindow() },
                )
                Item(
                    "Check for Updates...",
                    onClick = { updateManager.checkForUpdates(showUI = true) },
                )
                Separator()
                Item(
                    stringResource(Res.string.Desktop_Quit),
                    onClick = {
                        updateManager.cleanup()
                        exitApplication()
                        instanceManager.shutdown()
                    },
                )
            },
        )
    }
}

@Composable
private fun trayIcon(): DrawableResource {
    val isDarkTheme = isSystemInDarkMode()
    val isWindows =
        (dependencies.platformInfo.platform as? Platform.Desktop)?.os == DesktopOS.Windows
    val runBackgroundState by dependencies.runBackgroundStateManager
        .observeState()
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

@Composable
private fun RunBackgroundState.text(): String =
    when (this) {
        is RunBackgroundState.RunningTests ->
            stringResource(Res.string.Dashboard_Running_Running) + " " + testType?.displayName

        RunBackgroundState.Stopping ->
            stringResource(Res.string.Dashboard_Running_Stopping_Title)

        is RunBackgroundState.UploadingMissingResults ->
            stringResource(
                Res.string.Results_UploadingMissing,
                (this.state as? UploadMissingMeasurements.State.Uploading)?.progressText.orEmpty(),
            )

        else ->
            ""
    }
