package org.ooni.probe.shared

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope

/**
 * Enhanced UpdateManager factory that creates managers with comprehensive error handling
 */
class EnhancedUpdateManagerFactory {
    companion object {
        /**
         * Creates an UpdateManager with full error handling, recovery, and diagnostics
         */
        fun createWithErrorHandling(
            platform: Platform,
            scope: CoroutineScope? = null,
            enableAutoRecovery: Boolean = true,
            enableDiagnostics: Boolean = true,
        ): EnhancedUpdateManager {
            val baseManager = createUpdateManager(platform)
            return EnhancedUpdateManager(
                baseManager = baseManager,
                scope = scope,
                enableAutoRecovery = enableAutoRecovery,
                enableDiagnostics = enableDiagnostics,
            )
        }
    }
}

/**
 * Enhanced UpdateManager wrapper that provides comprehensive error handling
 */
class EnhancedUpdateManager(
    private val baseManager: UpdateManager,
    scope: CoroutineScope? = null,
    enableAutoRecovery: Boolean = true,
    enableDiagnostics: Boolean = true,
) : UpdateManager {
    private val errorRecovery: UpdateErrorRecovery? = if (enableAutoRecovery && scope != null) {
        UpdateErrorRecovery(baseManager, scope)
    } else {
        null
    }

    private val diagnostics: UpdateDiagnosticsCollector? = if (enableDiagnostics) {
        UpdateDiagnosticsCollector()
    } else {
        null
    }

    init {
        Logger.i("EnhancedUpdateManager created with recovery=${errorRecovery != null}, diagnostics=${diagnostics != null}")

        // Set up comprehensive error logging
        baseManager.setErrorCallback { error ->
            Logger.e("UpdateManager Error: [${error.operation}] ${error.message} (code: ${error.code})")
            logDiagnosticsOnError()
        }

        baseManager.setStateCallback { state ->
            Logger.d("UpdateManager State: $state")
            when (state) {
                UpdateState.CHECKING_FOR_UPDATES -> diagnostics?.trackUpdateCheckAttempt()
                UpdateState.NO_UPDATE_AVAILABLE, UpdateState.UPDATE_AVAILABLE -> diagnostics?.recordSuccessfulCheck()
                else -> { /* no action needed */ }
            }
        }
    }

    override fun initialize(
        appcastUrl: String,
        publicKey: String?,
    ) {
        diagnostics?.trackInitializationAttempt()
        Logger.i("Initializing UpdateManager with URL: $appcastUrl")
        baseManager.initialize(appcastUrl, publicKey)
    }

    override fun checkForUpdates(showUI: Boolean) {
        Logger.i("Checking for updates (showUI: $showUI)")
        baseManager.checkForUpdates(showUI)
    }

    override fun setAutomaticUpdatesEnabled(enabled: Boolean) {
        Logger.i("Setting automatic updates: $enabled")
        baseManager.setAutomaticUpdatesEnabled(enabled)
    }

    override fun setUpdateCheckInterval(hours: Int) {
        Logger.i("Setting update check interval: $hours hours")
        baseManager.setUpdateCheckInterval(hours)
    }

    override fun cleanup() {
        Logger.i("Cleaning up UpdateManager")
        errorRecovery?.cancel()
        baseManager.cleanup()
    }

    override fun setErrorCallback(callback: UpdateErrorCallback?) {
        // We handle error callbacks internally, but allow external callbacks too
        baseManager.setErrorCallback { error ->
            callback?.invoke(error)
        }
    }

    override fun setStateCallback(callback: UpdateStateCallback?) {
        // We handle state callbacks internally, but allow external callbacks too
        baseManager.setStateCallback { state ->
            callback?.invoke(state)
        }
    }

    override fun getLastError(): UpdateError? = baseManager.getLastError()

    override fun getCurrentState(): UpdateState = baseManager.getCurrentState()

    override fun retryLastOperation() {
        Logger.i("Retrying last operation")
        baseManager.retryLastOperation()
    }

    override fun isHealthy(): Boolean = baseManager.isHealthy()

    // Enhanced functionality

    /**
     * Force an immediate retry with error recovery
     */
    fun forceRetryWithRecovery() {
        errorRecovery?.forceRetry() ?: retryLastOperation()
    }

    /**
     * Get comprehensive diagnostics
     */
    fun getDiagnostics(): UpdateDiagnostics? = diagnostics?.collectDiagnostics(baseManager)

    /**
     * Export diagnostics as JSON
     */
    fun exportDiagnosticsJson(): String? = diagnostics?.exportDiagnosticsJson(baseManager)

    /**
     * Log comprehensive diagnostics
     */
    fun logDiagnostics() {
        diagnostics?.logDiagnosticsSummary(baseManager)
    }

    /**
     * Check if error recovery is active
     */
    fun isRecovering(): Boolean = errorRecovery?.isRecovering() ?: false

    /**
     * Get recovery statistics
     */
    fun getRecoveryStats(): Pair<Int, Int>? = errorRecovery?.getRetryStats()

    /**
     * Perform a comprehensive health check
     */
    fun performHealthCheck(): HealthCheckResult {
        val diagnostics = getDiagnostics()
        val isHealthy = isHealthy()
        val isRecovering = isRecovering()
        val lastError = getLastError()

        return HealthCheckResult(
            isHealthy = isHealthy,
            isRecovering = isRecovering,
            currentState = getCurrentState(),
            lastError = lastError,
            diagnostics = diagnostics,
            recommendations = diagnostics?.recommendedActions ?: emptyList(),
        )
    }

    private fun logDiagnosticsOnError() {
        try {
            diagnostics?.logDiagnosticsSummary(baseManager)
        } catch (e: Exception) {
            Logger.w("Failed to log diagnostics: $e")
        }
    }
}

data class HealthCheckResult(
    val isHealthy: Boolean,
    val isRecovering: Boolean,
    val currentState: UpdateState,
    val lastError: UpdateError?,
    val diagnostics: UpdateDiagnostics?,
    val recommendations: List<String>,
)
