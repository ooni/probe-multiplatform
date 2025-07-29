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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
import org.ooni.probe.shared.EnhancedUpdateManagerFactory
import org.ooni.probe.shared.InstanceManager
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.UpdateError
import org.ooni.probe.shared.UpdateState
import co.touchlab.kermit.Logger
import java.awt.Desktop
import java.awt.Dimension

const val APP_ID = "org.ooni.probe" // needs to be the same as conveyor `app.rdns-name`
const val APPCAST_URL = "https://25taexi16x.sharedwithexpose.com/appcast-aarch64.rss"
const val SPARKLE_PUBLIC_KEY = "pfIShU4dEXqPd5ObYNfDBiQWcXozk7estwzTnF9BamQ="

fun main(args: Array<String>) {
    val autoLaunch = AutoLaunch(appPackageName = APP_ID)
    val instanceManager = InstanceManager(dependencies.platformInfo)
    val deepLinkFlow = MutableSharedFlow<DeepLink?>(extraBufferCapacity = 1)
    
    // Create enhanced update manager with comprehensive error handling
    val updateManagerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val updateManager = EnhancedUpdateManagerFactory.createWithErrorHandling(
        platform = dependencies.platformInfo.platform,
        scope = updateManagerScope,
        enableAutoRecovery = true,
        enableDiagnostics = true
    )
    
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
                                updateManager.forceRetryWithRecovery()
                            }
                            updateManager.isHealthy() -> {
                                updateManager.checkForUpdates(showUI = true)
                            }
                            else -> {
                                Logger.w("Update system unhealthy, performing health check")
                                val health = updateManager.performHealthCheck()
                                Logger.i("Health check: healthy=${health.isHealthy}, recovering=${health.isRecovering}")
                                if (health.isHealthy) {
                                    updateManager.checkForUpdates(showUI = true)
                                }
                            }
                        }
                    },
                )
                // Advanced update options (only show when there are issues)
                if (updateSystemError != null || !updateManager.isHealthy()) {
                    Item(
                        "Update System Diagnostics",
                        onClick = { 
                            Logger.i("=== Update System Diagnostics ===")
                            updateManager.logDiagnostics()
                            val health = updateManager.performHealthCheck()
                            Logger.i("Health: ${health.isHealthy}, Recovering: ${health.isRecovering}")
                            health.recommendations.forEach { rec ->
                                Logger.i("Recommendation: $rec")
                            }
                            Logger.i("=== End Diagnostics ===")
                        },
                    )
                    if (updateManager.isRecovering()) {
                        val (current, max) = updateManager.getRecoveryStats() ?: (0 to 0)
                        Item(
                            text = "Recovery in Progress ($current/$max)",
                            enabled = false,
                            onClick = {},
                        )
                    }
                }
                Separator()
                Item(
                    stringResource(Res.string.Desktop_Quit),
                    onClick = {
                        Logger.i("Application shutdown initiated")
                        
                        // Cleanup update manager with diagnostics
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val health = updateManager.performHealthCheck()
                                Logger.i("Final health check: healthy=${health.isHealthy}")
                                
                                updateManager.cleanup()
                                updateManagerScope.cancel()
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
 * Sets up the update manager with comprehensive error handling and monitoring
 */
private suspend fun setupUpdateManager(
    updateManager: org.ooni.probe.shared.EnhancedUpdateManager,
    onStateChanged: (UpdateError?, UpdateState) -> Unit
) {
    // Set up error callback
    updateManager.setErrorCallback { error ->
        Logger.e("Update system error: ${error.message} (code: ${error.code})")
        onStateChanged(error, updateManager.getCurrentState())
        
        // Handle specific error types
        when (error.code) {
            -3, -4 -> {
                Logger.e("EdDSA key validation failed. Please check SPARKLE_PUBLIC_KEY configuration.")
                Logger.e("Current key: ${SPARKLE_PUBLIC_KEY.take(20)}...")
                // In production, you might want to show a user notification
            }
            -1 -> {
                Logger.w("Network or URL error. Update check will be retried automatically.")
                Logger.w("Appcast URL: $APPCAST_URL")
            }
            -999 -> {
                Logger.e("Update system setup or validation error: ${error.message}")
            }
            else -> {
                Logger.w("General update error (${error.code}): ${error.message}")
                Logger.w("Automatic recovery will be attempted if possible.")
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
    Logger.i("Appcast URL: $APPCAST_URL")
    Logger.i("Public key configured: ${SPARKLE_PUBLIC_KEY.isNotEmpty()}")
    
    updateManager.initialize(APPCAST_URL, SPARKLE_PUBLIC_KEY)
    
    // Configure automatic updates
    updateManager.setAutomaticUpdatesEnabled(true)
    updateManager.setUpdateCheckInterval(24) // Check every 24 hours
    
    // Perform initial health check
    val initialHealth = updateManager.performHealthCheck()
    Logger.i("Initial health check: healthy=${initialHealth.isHealthy}, state=${initialHealth.currentState}")
    
    if (!initialHealth.isHealthy) {
        Logger.w("Update system is not healthy:")
        initialHealth.recommendations.forEach { recommendation ->
            Logger.w("  - $recommendation")
        }
        
        // Export diagnostics for debugging
        val diagnostics = updateManager.exportDiagnosticsJson()
        diagnostics?.let { json ->
            Logger.d("Update diagnostics: $json")
        }
    }
}

/**
 * Generates appropriate menu text based on update system state
 */
private fun getUpdateMenuText(state: UpdateState, error: UpdateError?): String {
    return when {
        error != null -> "Retry Update Check (Error: ${error.code})"
        state == UpdateState.CHECKING_FOR_UPDATES -> "Checking for Updates..."
        state == UpdateState.UPDATE_AVAILABLE -> "Update Available!"
        state == UpdateState.NO_UPDATE_AVAILABLE -> "Check for Updates (Up to date)"
        state == UpdateState.INITIALIZING -> "Initializing Updates..."
        state == UpdateState.ERROR -> "Update System Error - Retry"
        else -> "Check for Updates..."
    }
}
