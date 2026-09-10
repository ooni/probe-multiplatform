package org.ooni.probe.domain.auth

import co.touchlab.kermit.Logger
import org.ooni.engine.DeleteResult

class ClearSession(
    private val deleteSecureStorage: suspend (String) -> DeleteResult,
) {
    suspend operator fun invoke() {
        try {
            deleteSecureStorage(AuthConstants.STORAGE_KEY)
            Logger.i("Cleared stored auth session")
        } catch (e: Exception) {
            Logger.w("Failed to clear auth session", e)
        }
    }
}
