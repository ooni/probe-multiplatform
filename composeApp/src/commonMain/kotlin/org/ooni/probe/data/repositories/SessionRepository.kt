package org.ooni.probe.data.repositories

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.Json
import org.ooni.engine.DeleteResult
import org.ooni.engine.WriteResult
import org.ooni.probe.data.models.AuthSession
import org.ooni.probe.data.models.SessionState
import org.ooni.probe.domain.auth.AuthConstants

class SessionRepository(
    private val readSecureStorage: suspend (String) -> String?,
    private val writeSecureStorage: suspend (String, String) -> WriteResult,
    private val deleteSecureStorage: suspend (String) -> DeleteResult,
    private val json: Json,
) {
    private val sessionUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val sessionState: Flow<SessionState> =
        sessionUpdates
            .onStart { emit(Unit) }
            .map {
                try {
                    val bodyText = readSecureStorage(AuthConstants.STORAGE_KEY)
                    if (bodyText.isNullOrEmpty()) {
                        SessionState.LoggedOut
                    } else {
                        val session = json.decodeFromString<AuthSession>(bodyText)
                        SessionState.LoggedIn(session)
                    }
                } catch (e: Exception) {
                    Logger.w("Failed to read auth session from secure storage", e)
                    SessionState.LoggedOut
                }
            }.distinctUntilChanged()

    suspend fun store(session: AuthSession): Boolean =
        try {
            val bodyText = json.encodeToString(AuthSession.serializer(), session)
            when (val result = writeSecureStorage(AuthConstants.STORAGE_KEY, bodyText)) {
                is WriteResult.Created, is WriteResult.Updated -> {
                    sessionUpdates.tryEmit(Unit)
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
            deleteSecureStorage(AuthConstants.STORAGE_KEY)
            sessionUpdates.tryEmit(Unit)
            Logger.i("Cleared stored auth session")
        } catch (e: Exception) {
            Logger.w("Failed to clear auth session", e)
        }
    }
}
