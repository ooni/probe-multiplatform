package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import org.ooni.engine.DeleteResult

class ClearCredential(
    private val deleteSecureStorage: suspend (String) -> DeleteResult,
) {
    suspend operator fun invoke() {
        try {
            deleteSecureStorage(CredentialsConstants.STORAGE_KEY)
            Logger.i("Cleared stored credential")
        } catch (e: Exception) {
            Logger.w("Failed to clear credential", e)
        }
    }
}
