package org.ooni.probe.shared

data class UpdateError(
    val code: Int,
    val message: String,
    val operation: String,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class UpdateState {
    IDLE,
    INITIALIZING,
    CHECKING_FOR_UPDATES,
    ERROR,
    UPDATE_AVAILABLE,
    NO_UPDATE_AVAILABLE,
}

enum class UpdateLogLevel(
    val value: Int,
) {
    DEBUG(0),
    INFO(1),
    WARN(2),
    ERROR(3),
}

data class UpdateLogMessage(
    val level: UpdateLogLevel,
    val operation: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
)

typealias UpdateErrorCallback = (UpdateError) -> Unit
typealias UpdateStateCallback = (UpdateState) -> Unit
typealias UpdateLogCallback = (UpdateLogMessage) -> Unit

interface UpdateManager {
    fun initialize(
        appcastUrl: String,
        publicKey: String? = null,
    )

    fun checkForUpdates(showUI: Boolean = false)

    fun setAutomaticUpdatesEnabled(enabled: Boolean)

    fun setUpdateCheckInterval(hours: Int)

    fun cleanup()

    // Error and state management
    fun setErrorCallback(callback: UpdateErrorCallback?)

    fun setStateCallback(callback: UpdateStateCallback?)

    fun getLastError(): UpdateError?

    fun getCurrentState(): UpdateState

    fun retryLastOperation()

    fun isHealthy(): Boolean

    // Logging functionality
    fun setLogCallback(callback: UpdateLogCallback?)
}

class NoOpUpdateManager : UpdateManager {
    private var lastError: UpdateError? = null
    private var currentState: UpdateState = UpdateState.IDLE
    private var errorCallback: UpdateErrorCallback? = null
    private var stateCallback: UpdateStateCallback? = null
    private var logCallback: UpdateLogCallback? = null

    override fun initialize(
        appcastUrl: String,
        publicKey: String?,
    ) {}

    override fun checkForUpdates(showUI: Boolean) {}

    override fun setAutomaticUpdatesEnabled(enabled: Boolean) {}

    override fun setUpdateCheckInterval(hours: Int) {}

    override fun cleanup() {}

    override fun setErrorCallback(callback: UpdateErrorCallback?) {
        errorCallback = callback
    }

    override fun setStateCallback(callback: UpdateStateCallback?) {
        stateCallback = callback
    }

    override fun getLastError(): UpdateError? = lastError

    override fun getCurrentState(): UpdateState = currentState

    override fun retryLastOperation() {}

    override fun isHealthy(): Boolean = true

    override fun setLogCallback(callback: UpdateLogCallback?) {
        logCallback = callback
    }
}
