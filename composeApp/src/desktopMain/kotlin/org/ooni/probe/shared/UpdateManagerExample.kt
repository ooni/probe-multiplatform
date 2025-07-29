package org.ooni.probe.shared

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Example implementation showing how to use the enhanced UpdateManager with comprehensive error handling
 */
class UpdateManagerExample {
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var updateManager: EnhancedUpdateManager
    
    fun initialize() {
        // Create an enhanced update manager with full error handling
        updateManager = EnhancedUpdateManagerFactory.createWithErrorHandling(
            platform = detectPlatform(),
            scope = scope,
            enableAutoRecovery = true,
            enableDiagnostics = true
        )
        
        // Set up custom error handling
        updateManager.setErrorCallback { error ->
            handleUpdateError(error)
        }
        
        // Set up state monitoring
        updateManager.setStateCallback { state ->
            handleStateChange(state)
        }
        
        // Initialize with your appcast URL and public key
        val appcastUrl = "https://example.com/updates/appcast.xml"
        val publicKey = "your-base64-encoded-eddsa-public-key-here="
        
        Logger.i("Initializing update system...")
        updateManager.initialize(appcastUrl, publicKey)
        
        // Perform initial health check
        performHealthCheck()
        
        // Configure automatic updates
        updateManager.setAutomaticUpdatesEnabled(true)
        updateManager.setUpdateCheckInterval(24) // Check daily
    }
    
    fun checkForUpdates() {
        Logger.i("Manually checking for updates...")
        updateManager.checkForUpdates(showUI = true)
    }
    
    fun performHealthCheck() {
        val healthResult = updateManager.performHealthCheck()
        
        Logger.i("=== Update System Health Check ===")
        Logger.i("Healthy: ${healthResult.isHealthy}")
        Logger.i("Recovering: ${healthResult.isRecovering}")
        Logger.i("State: ${healthResult.currentState}")
        
        healthResult.lastError?.let { error ->
            Logger.w("Last Error: ${error.code} - ${error.message}")
        }
        
        if (healthResult.recommendations.isNotEmpty()) {
            Logger.i("Recommendations:")
            healthResult.recommendations.forEach { recommendation ->
                Logger.i("  - $recommendation")
            }
        }
        
        Logger.i("=== End Health Check ===")
    }
    
    fun exportDiagnostics(): String? {
        return updateManager.exportDiagnosticsJson()
    }
    
    fun retryWithRecovery() {
        Logger.i("Attempting retry with error recovery...")
        updateManager.forceRetryWithRecovery()
    }
    
    private fun handleUpdateError(error: UpdateError) {
        Logger.e("Update error occurred: ${error.message} (code: ${error.code})")
        
        when (error.code) {
            -3, -4 -> {
                // Key-related errors
                Logger.e("Public key issue detected. Please verify your EdDSA key.")
                showUserMessage("Update configuration error. Please contact support.")
            }
            -1 -> {
                // Network/URL errors
                Logger.e("Network or URL issue detected.")
                showUserMessage("Unable to check for updates. Please check your internet connection.")
            }
            else -> {
                Logger.e("General update error: ${error.message}")
                showUserMessage("Update system encountered an error. Attempting automatic recovery...")
            }
        }
        
        // Log detailed diagnostics on error
        updateManager.logDiagnostics()
    }
    
    private fun handleStateChange(state: UpdateState) {
        when (state) {
            UpdateState.INITIALIZING -> {
                Logger.i("Update system initializing...")
                showUserMessage("Initializing update system...")
            }
            UpdateState.CHECKING_FOR_UPDATES -> {
                Logger.i("Checking for updates...")
                showUserMessage("Checking for updates...")
            }
            UpdateState.UPDATE_AVAILABLE -> {
                Logger.i("Update available!")
                showUserMessage("A new version is available!")
            }
            UpdateState.NO_UPDATE_AVAILABLE -> {
                Logger.i("No updates available")
                showUserMessage("You're running the latest version.")
            }
            UpdateState.ERROR -> {
                Logger.w("Update system in error state")
                val isRecovering = updateManager.isRecovering()
                if (isRecovering) {
                    showUserMessage("Update error occurred. Attempting recovery...")
                } else {
                    showUserMessage("Update system error. Please try again later.")
                }
            }
            UpdateState.IDLE -> {
                Logger.d("Update system idle")
            }
        }
    }
    
    private fun showUserMessage(message: String) {
        // In a real app, this would show a toast, notification, or update the UI
        Logger.i("USER MESSAGE: $message")
    }
    
    private fun detectPlatform(): Platform {
        val osName = System.getProperty("os.name", "").lowercase()
        return Platform.Desktop(osName)
    }
    
    fun cleanup() {
        Logger.i("Cleaning up update manager...")
        updateManager.cleanup()
    }
    
    // Debugging and troubleshooting methods
    
    fun debugUpdateSystem() {
        Logger.i("=== Debug Information ===")
        
        val diagnostics = updateManager.getDiagnostics()
        diagnostics?.let { diag ->
            Logger.i("Platform: ${diag.platform}")
            Logger.i("Library Loaded: ${diag.libraryLoaded}")
            Logger.i("Current State: ${diag.currentState}")
            Logger.i("Initialization Attempts: ${diag.initializationAttempts}")
            Logger.i("Update Check Attempts: ${diag.updateCheckAttempts}")
            Logger.i("Network Connectivity: ${diag.networkConnectivity}")
            
            diag.lastError?.let { error ->
                Logger.i("Last Error: ${error.code} - ${error.message} (${error.operation})")
            }
            
            Logger.i("Environment:")
            Logger.i("  OS: ${diag.environmentInfo.osName} ${diag.environmentInfo.osVersion}")
            diag.environmentInfo.sparkleVersion?.let { version ->
                Logger.i("  Sparkle Version: $version")
            }
            Logger.i("  WinSparkle Available: ${diag.environmentInfo.winsparkleAvailable}")
        }
        
        val recoveryStats = updateManager.getRecoveryStats()
        recoveryStats?.let { (current, max) ->
            Logger.i("Recovery: $current/$max attempts")
        }
        
        Logger.i("Is Healthy: ${updateManager.isHealthy()}")
        Logger.i("Is Recovering: ${updateManager.isRecovering()}")
        
        Logger.i("=== End Debug ===")
    }
    
    fun simulateNetworkError() {
        // For testing purposes - this would simulate a network error
        Logger.i("Simulating network error for testing...")
        // You would trigger an update check that fails here
    }
    
    fun testErrorRecovery() {
        Logger.i("Testing error recovery system...")
        
        // Force a retry
        updateManager.retryLastOperation()
        
        // Wait a bit and check recovery status
        val isRecovering = updateManager.isRecovering()
        Logger.i("Recovery active: $isRecovering")
        
        val stats = updateManager.getRecoveryStats()
        stats?.let { (current, max) ->
            Logger.i("Recovery attempts: $current/$max")
        }
    }
}