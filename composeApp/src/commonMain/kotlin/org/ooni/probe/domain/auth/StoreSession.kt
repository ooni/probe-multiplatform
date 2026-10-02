package org.ooni.probe.domain.auth

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import org.ooni.engine.WriteResult
import org.ooni.probe.data.models.AuthSession

class StoreSession(
    private val writeSecureStorage: suspend (String, String) -> WriteResult,
    private val json: Json,
) {
    suspend operator fun invoke(session: AuthSession): Boolean =
        try {
            val bodyText = json.encodeToString(AuthSession.serializer(), session)
            when (val result = writeSecureStorage(AuthConstants.STORAGE_KEY, bodyText)) {
                is WriteResult.Created, is WriteResult.Updated -> true
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
}
