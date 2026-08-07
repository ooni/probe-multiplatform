package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import org.ooni.probe.data.models.Credential

class GetCredential(
    private val readSecureStorage: suspend (String) -> String?,
    private val json: Json,
) {
    suspend operator fun invoke(): Credential? =
        try {
            val bodyText = readSecureStorage(CredentialsConstants.STORAGE_KEY) ?: return null
            json.decodeFromString<Credential>(bodyText)
        } catch (e: Exception) {
            Logger.w("Failed to read credentials from secure storage", e)
            null
        }
}
