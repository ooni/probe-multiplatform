package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import org.ooni.engine.WriteResult
import org.ooni.probe.data.models.Credential

class SetCredential(
    private val writeSecureStorage: suspend (String, String) -> WriteResult,
    private val json: Json,
) {
    suspend operator fun invoke(credential: Credential): Boolean =
        try {
            val bodyText = json.encodeToString(Credential.serializer(), credential)
            writeSecureStorage(CredentialsConstants.STORAGE_KEY, bodyText)
            Logger.i("User registered successfully, credentials stored")
            true
        } catch (e: Exception) {
            Logger.w("Failed to store credentials", e)
            false
        }
}
