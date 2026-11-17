package org.ooni.probe.data.repositories

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import org.ooni.engine.DeleteResult
import org.ooni.engine.WriteResult
import org.ooni.probe.data.models.AuthSession
import org.ooni.probe.data.models.SessionState
import org.ooni.probe.domain.auth.AuthConstants
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SessionRepository(
    private val readSecureStorage: suspend (String) -> String?,
    private val writeSecureStorage: suspend (String, String) -> WriteResult,
    private val deleteSecureStorage: suspend (String) -> DeleteResult,
    private val json: Json,
    private val readRetryDelay: Duration = 5.seconds,
) {
    private val sessionStateFlow = MutableStateFlow<SessionState?>(null)
    private val mutex = Mutex()

    /**
     * Emits the cached or freshly-loaded initial state, then collects subsequent updates from [sessionStateFlow].
     * If reading storage fails (e.g. iOS keychain locked), the read is retried every [readRetryDelay]
     * until it succeeds or [store]/[clear] sets the state.
     * [distinctUntilChanged] suppresses the duplicate emission from [sessionStateFlow]'s current value.
     */
    val sessionState: Flow<SessionState> =
        flow {
            emit(getOrLoadSessionState())
            while (sessionStateFlow.value == null) {
                val updated = withTimeoutOrNull(readRetryDelay) { sessionStateFlow.filterNotNull().first() }
                if (updated == null) emit(getOrLoadSessionState())
            }
            emitAll(sessionStateFlow.filterNotNull())
        }.distinctUntilChanged()

    private suspend fun getOrLoadSessionState(): SessionState {
        sessionStateFlow.value?.let { return it }
        return mutex.withLock {
            sessionStateFlow.value?.let { return it }
            val (state, success) = loadFromStorage()
            if (success) {
                // If a concurrent store() or clear() set the value while loading, compareAndSet fails and we use the newer state.
                sessionStateFlow.compareAndSet(null, state)
                sessionStateFlow.value!!
            } else {
                sessionStateFlow.value ?: state
            }
        }
    }

    private suspend fun loadFromStorage(): Pair<SessionState, Boolean> =
        try {
            val bodyText = readSecureStorage(AuthConstants.STORAGE_KEY)
            if (bodyText.isNullOrEmpty()) {
                SessionState.LoggedOut to true
            } else {
                val session = json.decodeFromString<AuthSession>(bodyText)
                SessionState.LoggedIn(session) to true
            }
        } catch (e: Exception) {
            Logger.w("Failed to read auth session from secure storage", e)
            SessionState.LoggedOut to false
        }

    suspend fun store(session: AuthSession): Boolean =
        try {
            val bodyText = json.encodeToString(AuthSession.serializer(), session)
            when (val result = writeSecureStorage(AuthConstants.STORAGE_KEY, bodyText)) {
                is WriteResult.Created, is WriteResult.Updated -> {
                    sessionStateFlow.value = SessionState.LoggedIn(session)
                    true
                }
                is WriteResult.Error -> {
                    Logger.w("Failed to store auth session in secure storage: ${result.message}", result.cause)
                    false
                }
                is WriteResult.TemporarilyUnavailable -> {
                    Logger.i("Secure storage temporarily unavailable, deferring session storage")
                    false
                }
            }
        } catch (e: Exception) {
            Logger.w("Failed to store auth session", e)
            false
        }

    suspend fun clear() {
        try {
            when (val result = deleteSecureStorage(AuthConstants.STORAGE_KEY)) {
                is DeleteResult.Deleted, is DeleteResult.NotFound -> {
                    sessionStateFlow.value = SessionState.LoggedOut
                    Logger.i("Cleared stored auth session")
                }
                is DeleteResult.Error -> {
                    Logger.w("Failed to clear auth session in secure storage: ${result.message}", result.cause)
                }
            }
        } catch (e: Exception) {
            Logger.w("Failed to clear auth session", e)
        }
    }
}
