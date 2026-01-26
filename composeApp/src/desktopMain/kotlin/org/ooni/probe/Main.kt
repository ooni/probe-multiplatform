package org.ooni.probe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.Logger
import io.github.kdroidfilter.platformtools.darkmodedetector.isSystemInDarkMode
import io.github.kdroidfilter.platformtools.darkmodedetector.windows.setWindowsAdaptiveTitleBar
import io.github.vinceglb.autolaunch.AutoLaunch
import java.awt.Desktop
import java.awt.desktop.AppReopenedListener
import java.awt.desktop.QuitEvent
import java.awt.desktop.QuitResponse
import java.awt.Dimension
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
import ooniprobe.composeapp.generated.resources.ooni_colored_logo
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
import org.ooni.probe.background.registerWindowsUrlScheme
import org.ooni.probe.data.models.DeepLink
import org.ooni.probe.data.models.RunBackgroundState
import org.ooni.probe.domain.UploadMissingMeasurements
import org.ooni.probe.shared.DeepLinkParser
import org.ooni.probe.shared.DesktopOS
import org.ooni.probe.shared.createUpdateManager
import org.ooni.probe.shared.InstanceManager
import org.ooni.probe.shared.MacDockVisibility
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.UpdateState
import org.ooni.probe.update.DesktopUpdateController
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Modal_Hide
import ooniprobe.composeapp.generated.resources.Modal_Quite_Description
import ooniprobe.composeapp.generated.resources.Modal_Quite_Prompt
import org.ooni.probe.ui.theme.AppTheme

const val APP_ID = "org.ooni.probe"

fun main(args: Array<String>) {
    val autoLaunch = AutoLaunch(appPackageName = APP_ID)
    val instanceManager = InstanceManager(dependencies.platformInfo)
    val deepLinkFlow = MutableSharedFlow<DeepLink?>(extraBufferCapacity = 1)

    // Create update manager and controller
    val updateManager = createUpdateManager(dependencies.platformInfo.platform)
    val updateController = DesktopUpdateController(updateManager)

    CoroutineScope(Dispatchers.IO).launch {
        instanceManager.observeUrls().collectLatest {
            deepLinkFlow.tryEmit(DeepLinkParser(it))
        }
    }

    instanceManager.initialize(args)

    // Allow views above SwingPanel for the webview
    // https://github.com/JetBrains/compose-multiplatform-core/pull/915
    System.setProperty("compose.interop.blending", "true")

    CoroutineScope(Dispatchers.Default).launch {
        autoLaunch.enable()
    }

    // Initialize update controller
    updateController.initialize(CoroutineScope(Dispatchers.Default))

    application {
        val appScope = rememberCoroutineScope()
        // Register shutdown callback for update installation when applicable
        updateController.registerShutdownHandler(this)
        var isWindowVisible by remember { mutableStateOf(!autoLaunch.isStartedViaAutostart()) }
        var showQuitPrompt by remember { mutableStateOf(false) }

        // Set initial dock visibility based on window visibility
        MacDockVisibility.setDockIconVisible(isWindowVisible)
        val trayIcon = trayIcon()
        val deepLink by deepLinkFlow.collectAsState(null)
        val runBackgroundState by dependencies.runBackgroundStateManager
            .observeState()
            .collectAsState(RunBackgroundState.Idle())

        // Observe update state for UI
        val updateState by updateController.state.collectAsState(UpdateState.IDLE)
        val updateError by updateController.error.collectAsState(null)

        fun showWindow() {
            isWindowVisible = true
            MacDockVisibility.showDockIcon()
            if (Desktop.isDesktopSupported() &&
                Desktop
                    .getDesktop()
                    .isSupported(Desktop.Action.APP_REQUEST_FOREGROUND)
            ) {
                Desktop.getDesktop().requestForeground(true)
            }
        }

        Window(
            onCloseRequest = {
                isWindowVisible = false
                MacDockVisibility.hideDockIcon()
            },
            visible = isWindowVisible,
            icon = painterResource(Res.drawable.ooni_colored_logo),
            title = stringResource(Res.string.app_name),
            state = rememberWindowState(
                size = DpSize(480.dp, 800.dp),
            ),
        ) {
            window.setWindowsAdaptiveTitleBar()
            window.minimumSize = Dimension(320, 560)
            window.maximumSize = Dimension(1024, 1024)

            AppTheme {
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

                if (showQuitPrompt) {
                    AppTheme {
                        QuitPromptDialog(
                            onQuit = {
                                showQuitPrompt = false
                                onQuitApplicationClicked(updateController, instanceManager).invoke()
                            },
                            onHide = {
                                showQuitPrompt = false
                                isWindowVisible = false
                                MacDockVisibility.hideDockIcon()
                            },
                            onDismiss = {
                                showQuitPrompt = false
                            },
                        )
                    }
                }
            }
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
                // Only show update UI on Mac and Windows platforms
                if (updateController.supportsUpdates()) {
                    Item(
                        text = stringResource(updateController.getMenuText()),
                        enabled = updateState != UpdateState.CHECKING_FOR_UPDATES,
                        onClick = { updateController.checkNow() },
                    )
                }
                Separator()
                Item(
                    stringResource(Res.string.Desktop_Quit),
                    onClick = {
                        showQuitPrompt = true
                        showWindow()
                    },
                )
            },
        )

        CoroutineScope(Dispatchers.Default).launch {
            registerWindowsUrlScheme()
        }

        LaunchedEffect(Unit) {
            runCatching {
                val desktop = Desktop.getDesktop()
                if (desktop.isSupported(Desktop.Action.APP_EVENT_FOREGROUND)) {
                    desktop.addAppEventListener(AppReopenedListener { showWindow() })
                }
                if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
                    desktop.setQuitHandler { _: QuitEvent?, response: QuitResponse ->
                        appScope.launch {
                            isWindowVisible = false
                        }
                    }
                }
            }.onFailure {
                Logger.w("Failed to register dock reopen listener", it)
            }
        }
    }
}

private fun ApplicationScope.onQuitApplicationClicked(
    updateController: DesktopUpdateController,
    instanceManager: InstanceManager,
): () -> Unit =
    {
        Logger.i("Application shutdown initiated")
        CoroutineScope(Dispatchers.IO).launch {
            updateController.cleanup()
            exitApplication()
            instanceManager.shutdown()
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

@Composable
private fun QuitPromptDialog(
    onQuit: () -> Unit,
    onHide: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.Modal_Quite_Prompt)) },
        text = { Text(stringResource(Res.string.Modal_Quite_Description)) },
        confirmButton = {
            TextButton(onClick = onQuit) { Text(stringResource(Res.string.Desktop_Quit)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onHide) { Text(stringResource(Res.string.Modal_Hide)) }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.Modal_Cancel)) }
            }
        },
    )
}
