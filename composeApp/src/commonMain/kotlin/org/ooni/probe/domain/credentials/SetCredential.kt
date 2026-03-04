package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import org.ooni.engine.WriteResult

class SetCredential(
    private val writeSecureStorage: suspend (String, String) -> WriteResult,
) {
    suspend operator fun invoke(credential: String): Boolean =
        try {
            writeSecureStorage(CredentialsConstants.STORAGE_KEY, credential)
            Logger.i("User registered successfully, credentials stored")
            true
        } catch (e: Exception) {
            Logger.w("Failed to store credentials", e)
            false
        }
}
