package org.ooni.probe.shared

import co.touchlab.kermit.Logger

data class UpdateDiagnostics(
    val platform: String,
    val libraryLoaded: Boolean,
    val currentState: String,
    val lastError: UpdateErrorInfo?,
    val initializationAttempts: Int,
    val updateCheckAttempts: Int,
    val lastSuccessfulCheck: Long?,
    val environmentInfo: EnvironmentInfo,
    val networkConnectivity: Boolean,
    val recommendedActions: List<String>
)

data class UpdateErrorInfo(
    val code: Int,
    val message: String,
    val operation: String,
    val timestamp: Long
)

data class EnvironmentInfo(
    val osName: String,
    val osVersion: String,
    val sparkleVersion: String?,
    val winsparkleAvailable: Boolean,
    val requiredLibrariesPresent: List<String>
)

/**
 * Comprehensive diagnostic system for UpdateManager
 * Provides detailed information about the update system state
 */
class UpdateDiagnosticsCollector {
    private var initializationAttempts = 0
    private var updateCheckAttempts = 0
    private var lastSuccessfulCheck: Long? = null
    
    fun collectDiagnostics(updateManager: UpdateManager): UpdateDiagnostics {
        Logger.d("Collecting update diagnostics...")
        
        val platform = detectPlatform()
        val lastError = updateManager.getLastError()
        
        return UpdateDiagnostics(
            platform = platform,
            libraryLoaded = checkLibraryLoaded(),
            currentState = updateManager.getCurrentState().name,
            lastError = lastError?.let { 
                UpdateErrorInfo(it.code, it.message, it.operation, it.timestamp) 
            },
            initializationAttempts = initializationAttempts,
            updateCheckAttempts = updateCheckAttempts,
            lastSuccessfulCheck = lastSuccessfulCheck,
            environmentInfo = collectEnvironmentInfo(platform),
            networkConnectivity = checkNetworkConnectivity(),
            recommendedActions = generateRecommendations(updateManager, platform)
        )
    }
    
    fun trackInitializationAttempt() {
        initializationAttempts++
    }
    
    fun trackUpdateCheckAttempt() {
        updateCheckAttempts++
    }
    
    fun recordSuccessfulCheck() {
        lastSuccessfulCheck = System.currentTimeMillis()
    }
    
    private fun detectPlatform(): String {
        val osName = System.getProperty("os.name", "unknown").lowercase()
        return when {
            osName.contains("mac") -> "macOS"
            osName.contains("win") -> "Windows"
            osName.contains("nix") || osName.contains("nux") -> "Linux"
            else -> osName
        }
    }
    
    private fun checkLibraryLoaded(): Boolean {
        return try {
            // This is a basic check - in practice you might want to call a simple native function
            System.getProperty("java.library.path") != null
        } catch (e: Exception) {
            false
        }
    }
    
    private fun collectEnvironmentInfo(platform: String): EnvironmentInfo {
        return EnvironmentInfo(
            osName = System.getProperty("os.name", "unknown"),
            osVersion = System.getProperty("os.version", "unknown"),
            sparkleVersion = if (platform == "macOS") detectSparkleVersion() else null,
            winsparkleAvailable = if (platform == "Windows") checkWinSparkleAvailable() else false,
            requiredLibrariesPresent = checkRequiredLibraries(platform)
        )
    }
    
    private fun detectSparkleVersion(): String? {
        // In practice, you might read this from the Sparkle framework
        return "2.5.2" // This would be detected dynamically
    }
    
    private fun checkWinSparkleAvailable(): Boolean {
        // This would check for WinSparkle.dll availability
        return true // Placeholder
    }
    
    private fun checkRequiredLibraries(platform: String): List<String> {
        val libraries = mutableListOf<String>()
        
        when (platform) {
            "macOS" -> {
                libraries.add("Sparkle.framework")
                libraries.add("updatebridge")
            }
            "Windows" -> {
                libraries.add("WinSparkle.dll")
                libraries.add("updatebridge")
            }
        }
        
        return libraries
    }
    
    private fun checkNetworkConnectivity(): Boolean {
        return try {
            // Basic connectivity check - you might want to ping the appcast URL
            val runtime = Runtime.getRuntime()
            val process = runtime.exec("ping -c 1 google.com")
            process.waitFor() == 0
        } catch (e: Exception) {
            Logger.w("Network connectivity check failed: $e")
            false
        }
    }
    
    private fun generateRecommendations(updateManager: UpdateManager, platform: String): List<String> {
        val recommendations = mutableListOf<String>()
        val lastError = updateManager.getLastError()
        val state = updateManager.getCurrentState()
        
        when (state) {
            UpdateState.ERROR -> {
                lastError?.let { error ->
                    when (error.code) {
                        -1 -> recommendations.add("Check appcast URL validity and network connectivity")
                        -2 -> recommendations.add("Restart application and retry initialization")
                        -3, -4 -> recommendations.add("Verify EdDSA public key format and regenerate if necessary")
                        -5 -> recommendations.add("Check application permissions and system compatibility")
                    }
                }
            }
            UpdateState.IDLE -> {
                if (initializationAttempts > 1) {
                    recommendations.add("Consider checking logs for initialization issues")
                }
            }
            else -> {
                if (updateCheckAttempts > 3) {
                    recommendations.add("Consider checking network connectivity and appcast server status")
                }
            }
        }
        
        when (platform) {
            "macOS" -> {
                recommendations.add("Ensure Sparkle.framework is properly embedded")
                recommendations.add("Check code signing and notarization status")
            }
            "Windows" -> {
                recommendations.add("Ensure WinSparkle.dll is in the application directory")
                recommendations.add("Check Windows Defender exclusions if needed")
            }
        }
        
        if (!checkNetworkConnectivity()) {
            recommendations.add("Network connectivity issues detected - check internet connection")
        }
        
        return recommendations.distinct()
    }
    
    fun exportDiagnosticsJson(updateManager: UpdateManager): String {
        val diagnostics = collectDiagnostics(updateManager)
        // Simple JSON-like string representation
        return buildString {
            appendLine("{")
            appendLine("  \"platform\": \"${diagnostics.platform}\",")
            appendLine("  \"libraryLoaded\": ${diagnostics.libraryLoaded},")
            appendLine("  \"currentState\": \"${diagnostics.currentState}\",")
            appendLine("  \"lastError\": ${diagnostics.lastError?.let { "\"${it.code}: ${it.message}\"" } ?: "null"},")
            appendLine("  \"initializationAttempts\": ${diagnostics.initializationAttempts},")
            appendLine("  \"updateCheckAttempts\": ${diagnostics.updateCheckAttempts},")
            appendLine("  \"networkConnectivity\": ${diagnostics.networkConnectivity},")
            append("  \"recommendedActions\": [")
            diagnostics.recommendedActions.forEachIndexed { index, action ->
                append("\"$action\"")
                if (index < diagnostics.recommendedActions.size - 1) append(", ")
            }
            appendLine("]")
            append("}")
        }
    }
    
    fun logDiagnosticsSummary(updateManager: UpdateManager) {
        val diagnostics = collectDiagnostics(updateManager)
        
        Logger.i("=== Update System Diagnostics ===")
        Logger.i("Platform: ${diagnostics.platform}")
        Logger.i("Library Loaded: ${diagnostics.libraryLoaded}")
        Logger.i("Current State: ${diagnostics.currentState}")
        Logger.i("Last Error: ${diagnostics.lastError?.let { "${it.code}: ${it.message}" } ?: "None"}")
        Logger.i("Initialization Attempts: ${diagnostics.initializationAttempts}")
        Logger.i("Update Check Attempts: ${diagnostics.updateCheckAttempts}")
        Logger.i("Network Connectivity: ${diagnostics.networkConnectivity}")
        
        if (diagnostics.recommendedActions.isNotEmpty()) {
            Logger.i("Recommended Actions:")
            diagnostics.recommendedActions.forEach { action ->
                Logger.i("  - $action")
            }
        }
        
        Logger.i("=== End Diagnostics ===")
    }
}