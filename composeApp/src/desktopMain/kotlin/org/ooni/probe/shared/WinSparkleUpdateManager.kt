package org.ooni.probe.shared

import co.touchlab.kermit.Logger

class WinSparkleUpdateManager(
    private val os: DesktopOS,
) : UpdateManager {
    init {
        loadLibrary(os)
    }

    private fun loadLibrary(os: DesktopOS) {
        try {
            val resourcesPath = System.getProperty("compose.application.resources.dir")
            // Load from resources directory
            val libraryPath = when (os) {
                DesktopOS.Mac -> "$resourcesPath/libupdatebridge.dylib"
                DesktopOS.Windows -> "$resourcesPath\\updatebridge.dll"
                else -> "$resourcesPath/libupdatebridge.so"
            }
            try {
                System.load(libraryPath)
                Logger.d("Successfully loaded updatebridge library from resources: $libraryPath")
            } catch (e: UnsatisfiedLinkError) {
                Logger.w("Failed to load updatebridge library from resources ($libraryPath), trying system library path:", e)
                System.loadLibrary("updatebridge")
            }
        } catch (e: UnsatisfiedLinkError) {
            Logger.e("Failed to load updatebridge library:", e)
        }
    }

    private external fun nativeInit(appcastUrl: String): Int

    private external fun nativeCheckForUpdates(showUI: Boolean): Int

    private external fun nativeSetAutomaticCheckEnabled(enabled: Boolean): Int

    private external fun nativeSetUpdateCheckInterval(hours: Int): Int

    private external fun nativeSetAppDetails(
        companyName: String,
        appName: String,
        appVersion: String,
    ): Int

    private external fun nativeCleanup(): Int

    // State management
    private var lastError: UpdateError? = null
    private var currentState: UpdateState = UpdateState.IDLE
    private var errorCallback: UpdateErrorCallback? = null
    private var stateCallback: UpdateStateCallback? = null
    private var lastOperation: (() -> Unit)? = null
    private var lastAppcastUrl: String? = null

    companion object {
        init {
            try {
                System.loadLibrary("updatebridge")
            } catch (e: UnsatisfiedLinkError) {
                Logger.e("Failed to load updatebridge library: ", e)
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
        Logger.e("WinSparkle Error [$operation]: $message (code: $code)")
    }

    override fun initialize(
        appcastUrl: String,
        publicKey: String?,
    ) {
        updateState(UpdateState.INITIALIZING)
        lastAppcastUrl = appcastUrl
        lastOperation = { initialize(appcastUrl, publicKey) }

        // Set app details from build config first
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

        val result = nativeInit(appcastUrl)
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

    override fun checkForUpdates(showUI: Boolean) {
        if (currentState == UpdateState.ERROR && !isHealthy()) {
            Logger.w("Cannot check for updates: WinSparkle is in error state")
            return
        }

        updateState(UpdateState.CHECKING_FOR_UPDATES)
        lastOperation = { checkForUpdates(showUI) }

        val result = nativeCheckForUpdates(showUI)
        when (result) {
            0 -> {
                Logger.d("Update check completed successfully")
                updateState(UpdateState.NO_UPDATE_AVAILABLE)
            }
            -1 -> reportError(result, "WinSparkle not initialized", "checkForUpdates")
            -2 -> reportError(result, "Exception occurred during update check", "checkForUpdates")
            else -> reportError(result, "Unknown error occurred during update check", "checkForUpdates")
        }
    }

    override fun setAutomaticUpdatesEnabled(enabled: Boolean) {
        lastOperation = { setAutomaticUpdatesEnabled(enabled) }

        val result = nativeSetAutomaticCheckEnabled(enabled)
        when (result) {
            0 -> Logger.d("Automatic updates ${if (enabled) "enabled" else "disabled"}")
            -1 -> reportError(result, "WinSparkle not initialized", "setAutomaticUpdatesEnabled")
            -2 -> reportError(result, "Exception occurred while setting automatic updates", "setAutomaticUpdatesEnabled")
            else -> reportError(result, "Unknown error occurred", "setAutomaticUpdatesEnabled")
        }
    }

    override fun setUpdateCheckInterval(hours: Int) {
        lastOperation = { setUpdateCheckInterval(hours) }

        val result = nativeSetUpdateCheckInterval(hours)
        when (result) {
            0 -> Logger.d("Update check interval set to $hours hours")
            -1 -> reportError(result, "WinSparkle not initialized", "setUpdateCheckInterval")
            -2 -> reportError(result, "Exception occurred while setting update interval", "setUpdateCheckInterval")
            else -> reportError(result, "Unknown error occurred", "setUpdateCheckInterval")
        }
    }

    override fun cleanup() {
        lastOperation = { cleanup() }

        val result = nativeCleanup()
        when (result) {
            0 -> {
                Logger.d("WinSparkle updater cleaned up successfully")
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
            Logger.i("Retrying last WinSparkle operation")
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
                error?.code in listOf(-1, -2) // Some initialization errors might be recoverable
            }
            else -> true
        }
}
