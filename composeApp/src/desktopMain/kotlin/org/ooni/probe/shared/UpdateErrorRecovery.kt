package org.ooni.probe.shared

import co.touchlab.kermit.Logger
import kotlinx.coroutines.*

/**
 * Automated error recovery system for UpdateManager
 * Handles common failure scenarios and implements retry logic
 */
class UpdateErrorRecovery(
    private val updateManager: UpdateManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private var retryJob: Job? = null
    private var retryCount = 0
    private val maxRetries = 3
    private val baseDelayMs = 1000L
    
    init {
        updateManager.setErrorCallback { error ->
            handleError(error)
        }
    }
    
    private fun handleError(error: UpdateError) {
        Logger.w("UpdateErrorRecovery: Handling error ${error.code} in ${error.operation}")
        
        when {
            isNetworkError(error) -> scheduleNetworkRetry(error)
            isInitializationError(error) -> scheduleInitializationRetry(error)
            isTransientError(error) -> scheduleTransientRetry(error)
            else -> {
                Logger.e("UpdateErrorRecovery: Non-recoverable error: ${error.message}")
                resetRetryCount()
            }
        }
    }
    
    private fun isNetworkError(error: UpdateError): Boolean {
        return error.operation == "checkForUpdates" && 
               error.message.contains("network", ignoreCase = true)
    }
    
    private fun isInitializationError(error: UpdateError): Boolean {
        return error.operation == "initialize" && error.code in listOf(-1, -2, -5)
    }
    
    private fun isTransientError(error: UpdateError): Boolean {
        return error.code == -2 // Exception errors might be transient
    }
    
    private fun scheduleNetworkRetry(error: UpdateError) {
        if (retryCount >= maxRetries) {
            Logger.e("UpdateErrorRecovery: Max network retries exceeded")
            resetRetryCount()
            return
        }
        
        val delay = calculateBackoffDelay()
        Logger.i("UpdateErrorRecovery: Scheduling network retry $retryCount/$maxRetries in ${delay}ms")
        
        retryJob?.cancel()
        retryJob = scope.launch {
            delay(delay)
            Logger.i("UpdateErrorRecovery: Attempting network retry")
            updateManager.retryLastOperation()
            retryCount++
        }
    }
    
    private fun scheduleInitializationRetry(error: UpdateError) {
        if (retryCount >= maxRetries) {
            Logger.e("UpdateErrorRecovery: Max initialization retries exceeded")
            resetRetryCount()
            return
        }
        
        val delay = calculateBackoffDelay()
        Logger.i("UpdateErrorRecovery: Scheduling initialization retry $retryCount/$maxRetries in ${delay}ms")
        
        retryJob?.cancel()
        retryJob = scope.launch {
            delay(delay)
            Logger.i("UpdateErrorRecovery: Attempting initialization retry")
            updateManager.retryLastOperation()
            retryCount++
        }
    }
    
    private fun scheduleTransientRetry(error: UpdateError) {
        if (retryCount >= 1) { // Only one retry for transient errors
            Logger.w("UpdateErrorRecovery: Transient error retry limit reached")
            resetRetryCount()
            return
        }
        
        Logger.i("UpdateErrorRecovery: Scheduling transient error retry")
        
        retryJob?.cancel()
        retryJob = scope.launch {
            delay(2000L) // Short delay for transient errors
            Logger.i("UpdateErrorRecovery: Attempting transient error retry")
            updateManager.retryLastOperation()
            retryCount++
        }
    }
    
    private fun calculateBackoffDelay(): Long {
        return baseDelayMs * (1 shl retryCount) // Exponential backoff: 1s, 2s, 4s
    }
    
    private fun resetRetryCount() {
        retryCount = 0
        retryJob?.cancel()
        retryJob = null
    }
    
    /**
     * Cancel any pending retry operations
     */
    fun cancel() {
        retryJob?.cancel()
        resetRetryCount()
    }
    
    /**
     * Force an immediate retry if possible
     */
    fun forceRetry() {
        if (updateManager.getLastError() != null) {
            Logger.i("UpdateErrorRecovery: Force retry requested")
            retryJob?.cancel()
            resetRetryCount()
            updateManager.retryLastOperation()
        }
    }
    
    /**
     * Check if the system is currently attempting recovery
     */
    fun isRecovering(): Boolean = retryJob?.isActive == true
    
    /**
     * Get current retry statistics
     */
    fun getRetryStats(): Pair<Int, Int> = retryCount to maxRetries
}