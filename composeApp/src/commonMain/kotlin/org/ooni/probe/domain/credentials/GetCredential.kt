package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.ooni.probe.data.models.Credential
import org.ooni.probe.data.models.SettingsKey

class GetCredential(
    private val readSecureStorage: suspend (String) -> String?,
    private val getPreference: (SettingsKey) -> Flow<Any?>,
) {
    suspend operator fun invoke(): Credential? =
        try {
           val emissionDay= getPreference(SettingsKey.EMISSION_DAY).first() as? UInt ?: return null

            val  credential = readSecureStorage(CredentialsConstants.STORAGE_KEY) ?: return null

            Credential(
                credential = credential,
                    emissionDay = emissionDay,
            )
        } catch (e: Exception) {
            Logger.w("Failed to read credentials from secure storage", e)
            null
        }
}
