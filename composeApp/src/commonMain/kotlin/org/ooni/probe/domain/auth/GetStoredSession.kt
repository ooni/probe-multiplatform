package org.ooni.probe.domain.auth

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import org.ooni.probe.data.models.AuthSession

class GetStoredSession(
    private val readSecureStorage: suspend (String) -> String?,
    private val json: Json,
) {
    suspend operator fun invoke(): AuthSession? =
        try {
            val bodyText = readSecureStorage(AuthConstants.STORAGE_KEY) ?: return null
            json.decodeFromString<AuthSession>(bodyText)
        } catch (e: Exception) {
            Logger.w("Failed to read auth session from secure storage", e)
            null
        }
}
