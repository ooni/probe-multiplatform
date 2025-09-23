package org.ooni.probe.shared

import org.ooni.shared.loadNativeLibrary

class WinSparkleUpdateManager : UpdateManager {
    companion object {
        private val isLibraryLoaded = loadNativeLibrary("updatebridge")
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

    private external fun nativeSetLogCallback(callback: Any?): Int

    private external fun nativeSetShutdownCallback(callback: Any?): Int

    private external fun nativeCleanup(): Int

    // Callback from native code for log messages
    @Suppress("unused")
    fun onLog(
        level: Int,
        operation: String,
        message: String,
    ) {
        val logLevel = UpdateLogLevel.values().find { it.value == level } ?: UpdateLogLevel.INFO
        val logMessage = UpdateLogMessage(logLevel, operation, message)
        logCallback?.invoke(logMessage)
    }

    // Callback from native code for shutdown requests
    @Suppress("unused")
    fun onShutdownRequested() {
        logCallback?.invoke(UpdateLogMessage(UpdateLogLevel.INFO, "shutdown", "Application shutdown requested by WinSparkle"))
        shutdownCallback?.invoke()
    }

    // State management
    private var lastError: UpdateError? = null
    private var currentState: UpdateState = UpdateState.IDLE
    private var errorCallback: UpdateErrorCallback? = null
    private var stateCallback: UpdateStateCallback? = null
    private var logCallback: UpdateLogCallback? = null
    private var shutdownCallback: (() -> Unit)? = null
    private var lastOperation: (() -> Unit)? = null
    private var lastAppcastUrl: String? = null

    private fun updateState(newState: UpdateState) {
        currentState = newState
        stateCallback?.invoke(newState)
    }

    private fun logErrorAndUpdateState(
        code: Int,
        message: String,
        operation: String,
    ) {
        val error = UpdateError(code, message, operation)
        lastError = error
        updateState(UpdateState.ERROR)
        errorCallback?.invoke(error)
        logCallback?.invoke(UpdateLogMessage(UpdateLogLevel.ERROR, operation, "$message (code: $code)"))
    }

    override fun initialize(
        appcastUrl: String,
        publicKey: String?,
    ) {
        updateState(UpdateState.INITIALIZING)
        lastAppcastUrl = appcastUrl
        lastOperation = { initialize(appcastUrl, publicKey) }

        val result = nativeInit(appcastUrl)
        when (result) {
            0 -> {
                logCallback?.invoke(UpdateLogMessage(UpdateLogLevel.INFO, "initialize", "WinSparkle updater initialized successfully"))
                updateState(UpdateState.IDLE)
            }
            -1 -> logErrorAndUpdateState(result, "Failed to load WinSparkle.dll", "initialize")
            -2 -> logErrorAndUpdateState(result, "Failed to load required WinSparkle functions", "initialize")
            else -> logErrorAndUpdateState(result, "Unknown error occurred", "initialize")
        }

        // Set app details from build config first
        val appDetailsResult = nativeSetAppDetails("OONI", "OONI Probe", "5.1.0")
        if (appDetailsResult != 0) {
            when (appDetailsResult) {
                -1 -> logErrorAndUpdateState(appDetailsResult, "WinSparkle not initialized for app details", "setAppDetails")
                -2 -> logErrorAndUpdateState(appDetailsResult, "Exception occurred while setting app details", "setAppDetails")
                -3 -> logErrorAndUpdateState(appDetailsResult, "win_sparkle_set_app_details function not available", "setAppDetails")
                -4 -> logErrorAndUpdateState(appDetailsResult, "Failed to convert strings to wide characters", "setAppDetails")
                else -> logErrorAndUpdateState(appDetailsResult, "Unknown error setting app details", "setAppDetails")
            }
            return
        }
    }

    override fun checkForUpdates(showUI: Boolean) {
        if (currentState == UpdateState.ERROR && !isHealthy()) {
            logCallback?.invoke(
                UpdateLogMessage(UpdateLogLevel.WARN, "checkForUpdates", "Cannot check for updates: WinSparkle is in error state"),
            )
            return
        }

        updateState(UpdateState.CHECKING_FOR_UPDATES)
        lastOperation = { checkForUpdates(showUI) }

        val result = nativeCheckForUpdates(showUI)
        when (result) {
            0 -> {
                logCallback?.invoke(UpdateLogMessage(UpdateLogLevel.INFO, "checkForUpdates", "Update check completed successfully"))
                updateState(UpdateState.NO_UPDATE_AVAILABLE)
            }
            -1 -> logErrorAndUpdateState(result, "WinSparkle not initialized", "checkForUpdates")
            -2 -> logErrorAndUpdateState(result, "Exception occurred during update check", "checkForUpdates")
            else -> logErrorAndUpdateState(result, "Unknown error occurred during update check", "checkForUpdates")
        }
    }

    override fun setAutomaticUpdatesEnabled(enabled: Boolean) {
        lastOperation = { setAutomaticUpdatesEnabled(enabled) }

        val result = nativeSetAutomaticCheckEnabled(enabled)
        when (result) {
            0 -> logCallback?.invoke(
                UpdateLogMessage(
                    UpdateLogLevel.INFO,
                    "setAutomaticUpdatesEnabled",
                    "Automatic updates ${if (enabled) "enabled" else "disabled"}",
                ),
            )
            -1 -> logErrorAndUpdateState(result, "WinSparkle not initialized", "setAutomaticUpdatesEnabled")
            -2 -> logErrorAndUpdateState(result, "Exception occurred while setting automatic updates", "setAutomaticUpdatesEnabled")
            else -> logErrorAndUpdateState(result, "Unknown error occurred", "setAutomaticUpdatesEnabled")
        }
    }

    override fun setUpdateCheckInterval(hours: Int) {
        lastOperation = { setUpdateCheckInterval(hours) }

        val result = nativeSetUpdateCheckInterval(hours)
        when (result) {
            0 -> logCallback?.invoke(
                UpdateLogMessage(UpdateLogLevel.INFO, "setUpdateCheckInterval", "Update check interval set to $hours hours"),
            )
            -1 -> logErrorAndUpdateState(result, "WinSparkle not initialized", "setUpdateCheckInterval")
            -2 -> logErrorAndUpdateState(result, "Exception occurred while setting update interval", "setUpdateCheckInterval")
            else -> logErrorAndUpdateState(result, "Unknown error occurred", "setUpdateCheckInterval")
        }
    }

    override fun cleanup() {
        lastOperation = { cleanup() }

        val result = nativeCleanup()
        when (result) {
            0 -> {
                logCallback?.invoke(UpdateLogMessage(UpdateLogLevel.INFO, "cleanup", "WinSparkle updater cleaned up successfully"))
                updateState(UpdateState.IDLE)
            }
            -2 -> logErrorAndUpdateState(result, "Exception occurred during cleanup", "cleanup")
            else -> logErrorAndUpdateState(result, "Unknown error occurred during cleanup", "cleanup")
        }
    }

    // Error and state management implementation
    override fun setErrorCallback(callback: UpdateErrorCallback?) {
        errorCallback = callback
    }

    override fun setStateCallback(callback: UpdateStateCallback?) {
        stateCallback = callback
    }

    override fun setLogCallback(callback: UpdateLogCallback?) {
        logCallback = callback
        // Set native callback - pass this object so native code can call onLog
        nativeSetLogCallback(if (callback != null) this else null)
    }

    fun setShutdownCallback(callback: (() -> Unit)?) {
        shutdownCallback = callback
        // Set native shutdown callback - pass this object so native code can call onShutdownRequested
        nativeSetShutdownCallback(if (callback != null) this else null)
    }

    override fun getLastError(): UpdateError? = lastError

    override fun getCurrentState(): UpdateState = currentState

    override fun retryLastOperation() {
        val operation = lastOperation
        if (operation != null) {
            logCallback?.invoke(UpdateLogMessage(UpdateLogLevel.INFO, "retryLastOperation", "Retrying last WinSparkle operation"))
            lastError = null
            operation.invoke()
        } else {
            logCallback?.invoke(UpdateLogMessage(UpdateLogLevel.WARN, "retryLastOperation", "No operation to retry"))
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
