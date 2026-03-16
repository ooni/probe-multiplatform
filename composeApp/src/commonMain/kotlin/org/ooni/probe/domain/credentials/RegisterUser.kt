package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.ooni.passport.PassportAuthRegister
import org.ooni.probe.config.BuildTypeDefaults
import org.ooni.probe.data.models.Credential
import kotlin.coroutines.CoroutineContext

class RegisterUser(
    private val passportAuthRegister: PassportAuthRegister,
    private val setCredential: suspend (Credential) -> Boolean,
    private val backgroundContext: CoroutineContext,
    private val json: Json,
) {
    suspend operator fun invoke(
        publicParams: String,
        manifestVersion: String,
    ): String? {
        return withContext(backgroundContext) {
            val url = "${BuildTypeDefaults.ooniApiBaseUrl}/api/v1/sign_credential"

            passportAuthRegister
                .userAuthRegister(url, publicParams, manifestVersion)
                .onSuccess { credentialResponse ->
                    if (!credentialResponse.response.isSuccessful) {
                        Logger.w("Failed to register user (status=${credentialResponse.response.statusCode})")
                        return@withContext null
                    }

                    val credential = credentialResponse.decodeCredential(json)
                    if (credential == null) {
                        Logger.w("Failed to register user (could not decode credential)")
                        return@withContext null
                    }

                    return@withContext if (setCredential(credential)) {
                        credential.credential
                    } else {
                        null
                    }
                }.onFailure { exception ->
                    Logger.w("Failed to register user", exception)
                }

            null
        }
    }
}
