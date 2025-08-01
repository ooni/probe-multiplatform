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
import org.ooni.probe.shared.createUpdateManager
import org.ooni.probe.shared.InstanceManager
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.UpdateError
import org.ooni.probe.shared.UpdateState
import org.ooni.probe.config.UpdateConfig
import co.touchlab.kermit.Logger
import java.awt.Desktop
import java.awt.Dimension

const val APP_ID = "org.ooni.probe" // needs to be the same as conveyor `app.rdns-name`

fun main(args: Array<String>) {
    val autoLaunch = AutoLaunch(appPackageName = APP_ID)
    val instanceManager = InstanceManager(dependencies.platformInfo)
    val deepLinkFlow = MutableSharedFlow<DeepLink?>(extraBufferCapacity = 1)

    // Create update manager
    val updateManager = createUpdateManager(dependencies.platformInfo.platform)

    // State for update system
    var updateSystemError by mutableStateOf<UpdateError?>(null)
    var updateSystemState by mutableStateOf(UpdateState.IDLE)

    CoroutineScope(Dispatchers.IO).launch {
        instanceManager.observeUrls().collectLatest {
            deepLinkFlow.tryEmit(DeepLinkParser(it))
        }
    }

    instanceManager.initialize(args)

    CoroutineScope(Dispatchers.Default).launch {
        autoLaunch.enable()
    }

    // Initialize update manager with comprehensive error handling
    CoroutineScope(Dispatchers.Default).launch {
        try {
            setupUpdateManager(updateManager) { error, state ->
                updateSystemError = error
                updateSystemState = state
            }
        } catch (e: Exception) {
            Logger.e("Failed to setup update manager: $e")
            updateSystemError = UpdateError(-999, "Setup failed: ${e.message}", "setup")
        }
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
                    text = getUpdateMenuText(updateSystemState, updateSystemError),
                    enabled = updateSystemState != UpdateState.CHECKING_FOR_UPDATES,
                    onClick = {
                        when {
                            updateSystemError != null -> {
                                Logger.i("Retrying update check after error")
                                updateManager.retryLastOperation()
                            }
                            updateManager.isHealthy() -> {
                                updateManager.checkForUpdates(showUI = true)
                            }
                            else -> {
                                Logger.w("Update system unhealthy, checking for updates anyway")
                                updateManager.checkForUpdates(showUI = true)
                            }
                        }
                    },
                )
                // Show retry option when there are errors
                if (updateSystemError != null) {
                    Item(
                        "Retry Update Check",
                        onClick = {
                            Logger.i("Manual retry requested")
                            updateManager.retryLastOperation()
                        },
                    )
                }
                Separator()
                Item(
                    stringResource(Res.string.Desktop_Quit),
                    onClick = {
                        Logger.i("Application shutdown initiated")

                        // Cleanup update manager
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                updateManager.cleanup()
                                Logger.i("Update manager cleaned up successfully")
                            } catch (e: Exception) {
                                Logger.e("Error during update manager cleanup: $e")
                            }

                            exitApplication()
                            instanceManager.shutdown()
                        }
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

/**
 * Sets up the update manager with basic error handling
 */
private suspend fun setupUpdateManager(
    updateManager: org.ooni.probe.shared.UpdateManager,
    onStateChanged: (UpdateError?, UpdateState) -> Unit,
) {
    // Set up error callback
    updateManager.setErrorCallback { error ->
        Logger.e("Update system error: ${error.message} (code: ${error.code})")
        onStateChanged(error, updateManager.getCurrentState())

        // Handle specific error types
        when (error.code) {
            -3, -4 -> {
                Logger.e("EdDSA key validation failed. Please check SPARKLE_PUBLIC_KEY configuration.")
                Logger.e("Current key: ${UpdateConfig.PUBLIC_KEY.take(20)}...")
            }
            -1 -> {
                Logger.w("Network or URL error. Update check will be retried automatically.")
                Logger.w("Appcast URL: ${UpdateConfig.URL}")
            }
            -999 -> {
                Logger.e("Update system setup or validation error: ${error.message}")
            }
            else -> {
                Logger.w("General update error (${error.code}): ${error.message}")
            }
        }
    }

    // Set up state callback
    updateManager.setStateCallback { state ->
        Logger.d("Update system state: $state")
        onStateChanged(updateManager.getLastError(), state)
    }

    // Initialize the update system
    Logger.i("Initializing update system...")
    Logger.i("Appcast URL: ${UpdateConfig.URL}")
    Logger.i("Public key configured: ${UpdateConfig.PUBLIC_KEY.isNotEmpty()}")

    updateManager.initialize(UpdateConfig.URL, UpdateConfig.PUBLIC_KEY)

    // Configure automatic updates
    updateManager.setAutomaticUpdatesEnabled(true)
    updateManager.setUpdateCheckInterval(24) // Check every 24 hours
}

/**
 * Generates appropriate menu text based on update system state
 */
private fun getUpdateMenuText(
    state: UpdateState,
    error: UpdateError?,
): String =
    when {
        error != null -> "Retry Update Check (Error: ${error.code})"
        state == UpdateState.CHECKING_FOR_UPDATES -> "Checking for Updates..."
        state == UpdateState.UPDATE_AVAILABLE -> "Update Available!"
        state == UpdateState.NO_UPDATE_AVAILABLE -> "Check for Updates (Up to date)"
        state == UpdateState.INITIALIZING -> "Initializing Updates..."
        state == UpdateState.ERROR -> "Update System Error - Retry"
        else -> "Check for Updates..."
    }
