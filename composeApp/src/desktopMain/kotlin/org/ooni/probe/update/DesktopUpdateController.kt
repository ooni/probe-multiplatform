package org.ooni.probe.update

import androidx.compose.ui.window.ApplicationScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.ooni.probe.config.UpdateConfig
import org.ooni.probe.dependencies
import org.ooni.probe.shared.DesktopOS
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.UpdateError
import org.ooni.probe.shared.UpdateManager
import org.ooni.probe.shared.UpdateState
import org.ooni.probe.shared.SparkleUpdateManager
import org.ooni.probe.shared.WinSparkleUpdateManager
import kotlin.collections.contains

/**
 * DesktopUpdateController encapsulates update logic for desktop targets.
 * Owns an [UpdateManager], exposes state and error as flows, and provides actions.
 */
class DesktopUpdateController(
    private val updateManager: UpdateManager,
) {
    private val _state = MutableStateFlow(UpdateState.IDLE)
    val state: StateFlow<UpdateState> = _state

    private val _error = MutableStateFlow<UpdateError?>(null)
    val error: StateFlow<UpdateError?> = _error

    /**
     * Initialize update manager and set callbacks. Safe to call multiple times.
     */
    fun initialize(scope: CoroutineScope) {
        if (updateManager is WinSparkleUpdateManager) {
            updateManager.setDllRoot(System.getProperty("compose.application.resources.dir") ?: "")
        }

        scope.launch(Dispatchers.Default) {
            try {
                updateManager.setErrorCallback { error ->
                    Logger.e("Update system error: ${error.message} (code: ${error.code})")
                    _error.value = error
                    _state.value = updateManager.getCurrentState()

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

                updateManager.setStateCallback { s ->
                    Logger.d("Update system state: $s")
                    _state.value = s
                    _error.value = updateManager.getLastError()
                }

                Logger.i("Initializing update system...")
                Logger.i("Appcast URL: ${UpdateConfig.URL}")
                Logger.i("Public key configured: ${UpdateConfig.PUBLIC_KEY.isNotEmpty()}")

                updateManager.initialize(UpdateConfig.URL, UpdateConfig.PUBLIC_KEY)
                updateManager.setAutomaticUpdatesEnabled(true)
                updateManager.setUpdateCheckInterval(24)
            } catch (e: Exception) {
                Logger.e("Failed to initialize update system: $e")
                _error.value = UpdateError(-999, "Setup failed: ${e.message}", "setup")
                _state.value = UpdateState.ERROR
            }
        }
    }

    fun registerShutdownHandler(appScope: ApplicationScope) {
        when (updateManager) {
            is WinSparkleUpdateManager -> updateManager.setShutdownCallback {
                Logger.i("WinSparkle requested application shutdown for update installation")
                appScope.exitApplication()
            }
            is SparkleUpdateManager -> updateManager.setShutdownCallback {
                Logger.i("Sparkle requested application shutdown for update installation")
                appScope.exitApplication()
            }
        }
    }

    fun checkNow() {
        when {
            _error.value != null -> {
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
    }

    fun retryLastOperation() {
        updateManager.retryLastOperation()
    }

    // TODO: add translations after macOS has been finalized
    fun getMenuText(): String =
        when (_state.value) {
            UpdateState.CHECKING_FOR_UPDATES -> "Checking for Updates..."
            UpdateState.UPDATE_AVAILABLE -> "Update Available!"
            else -> "Check for Updates"
        }

    suspend fun cleanup() {
        try {
            updateManager.cleanup()
            Logger.i("Update manager cleaned up successfully")
        } catch (e: Exception) {
            Logger.e("Error during update manager cleanup: $e")
        }
    }

    fun supportsUpdates(): Boolean =
        (dependencies.platformInfo.platform as? Platform.Desktop)?.os in listOf(DesktopOS.Mac, DesktopOS.Windows) && updateManager.isHealthy()
}
