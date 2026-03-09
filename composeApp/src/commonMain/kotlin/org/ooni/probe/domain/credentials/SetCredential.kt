package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import org.ooni.engine.WriteResult
import org.ooni.probe.data.models.SettingsKey

class SetCredential(
    private val writeSecureStorage: suspend (String, String) -> WriteResult,
    private val setPreference: suspend (SettingsKey, Any?) -> Unit,
) {
    suspend operator fun invoke(
        credential: String,
        emissionDay: UInt,
    ): Boolean =
        try {
            writeSecureStorage(CredentialsConstants.STORAGE_KEY, credential)
            setPreference(SettingsKey.EMISSION_DAY, emissionDay.toInt())
            Logger.i("User registered successfully, credentials stored")
            true
        } catch (e: Exception) {
            Logger.w("Failed to store credentials", e)
            false
        }
}
