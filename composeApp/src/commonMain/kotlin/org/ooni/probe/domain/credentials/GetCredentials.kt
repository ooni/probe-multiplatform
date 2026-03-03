package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

class GetCredentials(
    private val readCredentials: suspend (String) -> String?,
) {
    operator fun invoke(): Flow<String?> =
        flow {
            val credential = readCredentials(CredentialsConstants.CREDENTIALS_KEY)
            emit(credential)
        }.catch { e ->
            Logger.w("Failed to read credentials from secure storage", e)
            emit(null)
        }
}
