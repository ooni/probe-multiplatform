package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger

class GetCredential(
    private val readSecureStorage: suspend (String) -> String?,
) {
    suspend operator fun invoke(): String? =
        try {
            readSecureStorage(CredentialsConstants.STORAGE_KEY)
        } catch (e: Exception) {
            Logger.w("Failed to read credentials from secure storage", e)
            null
        }
}
