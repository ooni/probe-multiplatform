package org.ooni.probe.shared

import co.touchlab.kermit.Logger
import java.util.Base64

class SparkleUpdateManager(
    private val os: DesktopOS,
) : UpdateManager {
    init {
        loadLibrary(os)
    }

    private external fun nativeInit(
        appcastUrl: String,
        publicKey: String?,
    ): Int

    private external fun nativeCheckForUpdates(showUI: Boolean): Int

    private external fun nativeSetAutomaticCheckEnabled(enabled: Boolean): Int

    private external fun nativeSetUpdateCheckInterval(hours: Int): Int

    private external fun nativeSetUpdateCallback(callback: Long): Int

    private external fun nativeSetErrorCallback(callback: Long): Int

    private external fun nativeCleanup(): Int

    // Windows-specific native methods
    private external fun nativeSetAppDetails(
        companyName: String,
        appName: String,
        appVersion: String,
    ): Int

    // Native callback methods - called from C code
    @Suppress("unused")
    private fun onUpdateFound(
        version: String,
        description: String,
    ) {
        Logger.i("Update found: $version")
        updateState(UpdateState.UPDATE_AVAILABLE)
    }

    @Suppress("unused")
    private fun onUpdateNotFound() {
        Logger.i("No updates available")
        updateState(UpdateState.NO_UPDATE_AVAILABLE)
    }

    @Suppress("unused")
    private fun onUpdateError(
        errorCode: Int,
        errorMessage: String,
    ) {
        reportError(errorCode, errorMessage, "updateCheck")
    }

    // State management
    private var lastError: UpdateError? = null
    private var currentState: UpdateState = UpdateState.IDLE
    private var errorCallback: UpdateErrorCallback? = null
    private var stateCallback: UpdateStateCallback? = null
    private var lastOperation: (() -> Unit)? = null
    private var lastAppcastUrl: String? = null
    private var lastPublicKey: String? = null

    companion object {
        private var isLibraryLoaded = false

        private fun loadLibrary(os: DesktopOS) {
            if (isLibraryLoaded) return

            try {
                val resourcesPath = System.getProperty("compose.application.resources.dir")
                if (resourcesPath != null) {
                    // Load from resources directory
                    val libraryPath = when (os) {
                        DesktopOS.Mac -> "$resourcesPath/libupdatebridge.dylib"
                        DesktopOS.Windows -> "$resourcesPath/updatebridge.dll"
                        else -> "$resourcesPath/libupdatebridge.so"
                    }
                    try {
                        System.load(libraryPath)
                        Logger.d("Successfully loaded updatebridge library from resources: $libraryPath")
                        isLibraryLoaded = true
                    } catch (e: UnsatisfiedLinkError) {
                        Logger.w("Failed to load updatebridge library from resources ($libraryPath), trying system library path:", e)
                        System.loadLibrary("updatebridge")
                        isLibraryLoaded = true
                    }
                } else {
                    // Fallback to system library path
                    Logger.d("compose.application.resources.dir not set, using system library path")
                    System.loadLibrary("updatebridge")
                    isLibraryLoaded = true
                }
            } catch (e: UnsatisfiedLinkError) {
                Logger.e("Failed to load updatebridge library:", e)
            }
        }

        /**
         * Validates that a public key is properly formatted for Sparkle EdDSA verification
         * @param publicKey The base64-encoded public key string
         * @return true if the key is valid, false otherwise
         */
        private fun validatePublicKey(publicKey: String?): Boolean {
            if (publicKey.isNullOrBlank()) return true // null/empty is allowed

            return try {
                val decoded = Base64.getDecoder().decode(publicKey)
                if (decoded.size != 32) {
                    Logger.w("Public key has incorrect length: ${decoded.size} bytes (expected 32)")
                    return false
                }
                true
            } catch (e: IllegalArgumentException) {
                Logger.w("Public key is not valid base64: $e")
                false
            }
        }
    }

    private fun updateState(newState: UpdateState) {
        currentState = newState
        stateCallback?.invoke(newState)
    }

    private fun reportError(
        code: Int,
        message: String,
        operation: String,
    ) {
        val error = UpdateError(code, message, operation)
        lastError = error
        updateState(UpdateState.ERROR)
        errorCallback?.invoke(error)
        Logger.e("UpdateManager Error [$operation]: $message (code: $code)")
    }

    override fun initialize(
        appcastUrl: String,
        publicKey: String?,
    ) {
        updateState(UpdateState.INITIALIZING)
        lastAppcastUrl = appcastUrl
        lastPublicKey = publicKey
        lastOperation = { initialize(appcastUrl, publicKey) }

        when (os) {
            DesktopOS.Windows -> {
                // Windows-specific initialization: Set app details first
                val appDetailsResult = nativeSetAppDetails("OONI", "OONI Probe", "5.1.0")
                if (appDetailsResult != 0) {
                    when (appDetailsResult) {
                        -1 -> reportError(appDetailsResult, "WinSparkle not initialized for app details", "setAppDetails")
                        -2 -> reportError(appDetailsResult, "Exception occurred while setting app details", "setAppDetails")
                        -3 -> reportError(appDetailsResult, "win_sparkle_set_app_details function not available", "setAppDetails")
                        -4 -> reportError(appDetailsResult, "Failed to convert strings to wide characters", "setAppDetails")
                        else -> reportError(appDetailsResult, "Unknown error setting app details", "setAppDetails")
                    }
                    return
                }

                // Windows doesn't use public key validation
                val result = nativeInit(appcastUrl, null)
                when (result) {
                    0 -> {
                        Logger.d("WinSparkle updater initialized successfully")
                        updateState(UpdateState.IDLE)
                    }
                    -1 -> reportError(result, "Failed to load WinSparkle.dll", "initialize")
                    -2 -> reportError(result, "Failed to load required WinSparkle functions", "initialize")
                    else -> reportError(result, "Unknown error occurred", "initialize")
                }
            }
            else -> {
                // Mac/Linux: Validate public key format before passing to native code
                if (!validatePublicKey(publicKey)) {
                    reportError(-999, "Invalid public key format - initialization aborted", "initialize")
                    return
                }

                val result = nativeInit(appcastUrl, publicKey)
                when (result) {
                    0 -> {
                        Logger.d("Sparkle updater initialized successfully")
                        updateState(UpdateState.IDLE)
                    }
                    -1 -> reportError(result, "Invalid appcast URL", "initialize")
                    -2 -> reportError(result, "Failed to create updater controller", "initialize")
                    -3 -> reportError(result, "Invalid base64 encoding for public key (check for missing padding like '=')", "initialize")
                    -4 -> reportError(result, "Invalid key length (expected 32 bytes for EdDSA)", "initialize")
                    -5 -> reportError(result, "Exception occurred during initialization", "initialize")
                    else -> reportError(result, "Unknown error occurred", "initialize")
                }
            }
        }
    }

    override fun checkForUpdates(showUI: Boolean) {
        if (currentState == UpdateState.ERROR && !isHealthy()) {
            Logger.w("Cannot check for updates: UpdateManager is in error state")
            return
        }

        updateState(UpdateState.CHECKING_FOR_UPDATES)
        lastOperation = { checkForUpdates(showUI) }

        val result = nativeCheckForUpdates(showUI)
        when (result) {
            0 -> {
                Logger.d("Update check completed successfully")
                updateState(UpdateState.NO_UPDATE_AVAILABLE) // Will be updated by delegate if update is found
            }
            -1 -> reportError(result, "Updater not initialized", "checkForUpdates")
            -2 -> reportError(result, "Exception occurred during update check", "checkForUpdates")
            else -> reportError(result, "Unknown error occurred during update check", "checkForUpdates")
        }
    }

    override fun setAutomaticUpdatesEnabled(enabled: Boolean) {
        lastOperation = { setAutomaticUpdatesEnabled(enabled) }

        val result = nativeSetAutomaticCheckEnabled(enabled)
        when (result) {
            0 -> Logger.d("Automatic updates ${if (enabled) "enabled" else "disabled"}")
            -1 -> reportError(result, "Updater not initialized", "setAutomaticUpdatesEnabled")
            -2 -> reportError(result, "Exception occurred while setting automatic updates", "setAutomaticUpdatesEnabled")
            else -> reportError(result, "Unknown error occurred", "setAutomaticUpdatesEnabled")
        }
    }

    override fun setUpdateCheckInterval(hours: Int) {
        lastOperation = { setUpdateCheckInterval(hours) }

        val result = nativeSetUpdateCheckInterval(hours)
        when (result) {
            0 -> Logger.d("Update check interval set to $hours hours")
            -1 -> reportError(result, "Updater not initialized", "setUpdateCheckInterval")
            -2 -> reportError(result, "Exception occurred while setting update interval", "setUpdateCheckInterval")
            else -> reportError(result, "Unknown error occurred", "setUpdateCheckInterval")
        }
    }

    override fun cleanup() {
        lastOperation = { cleanup() }

        val result = nativeCleanup()
        when (result) {
            0 -> {
                Logger.d("Sparkle updater cleaned up successfully")
                updateState(UpdateState.IDLE)
            }
            -2 -> reportError(result, "Exception occurred during cleanup", "cleanup")
            else -> reportError(result, "Unknown error occurred during cleanup", "cleanup")
        }
    }

    // Error and state management implementation
    override fun setErrorCallback(callback: UpdateErrorCallback?) {
        errorCallback = callback
    }

    override fun setStateCallback(callback: UpdateStateCallback?) {
        stateCallback = callback
    }

    override fun getLastError(): UpdateError? = lastError

    override fun getCurrentState(): UpdateState = currentState

    override fun retryLastOperation() {
        val operation = lastOperation
        if (operation != null) {
            Logger.i("Retrying last operation")
            lastError = null
            operation.invoke()
        } else {
            Logger.w("No operation to retry")
        }
    }

    override fun isHealthy(): Boolean =
        when (currentState) {
            UpdateState.ERROR -> {
                val error = lastError
                // Consider recoverable if it's a network issue or temporary problem
                error?.code in listOf(-1, -2) // Initialization errors might be recoverable
            }
            else -> true
        }
}
