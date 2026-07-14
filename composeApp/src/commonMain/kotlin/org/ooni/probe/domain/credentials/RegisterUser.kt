package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.ooni.engine.models.Result
import org.ooni.passport.models.CredentialResponse
import org.ooni.passport.models.PassportException
import org.ooni.probe.config.BuildTypeDefaults
import org.ooni.probe.data.models.Credential
import kotlin.coroutines.CoroutineContext

class RegisterUser(
    private val userAuthRegister: suspend (
        url: String,
        publicParams: String,
        manifestVersion: String,
    ) -> Result<CredentialResponse, PassportException>,
    private val setCredential: suspend (Credential) -> Boolean,
    private val backgroundContext: CoroutineContext,
    private val json: Json,
) {
    suspend operator fun invoke(
        publicParams: String,
        manifestVersion: String,
    ): Credential? {
        return withContext(backgroundContext) {
            val url = "${BuildTypeDefaults.ooniApiBaseUrl}/api/v1/sign_credential"

            userAuthRegister(url, publicParams, manifestVersion)
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
                        credential
                    } else {
                        Logger.w("Failed to register user: could not store credential in secure storage")
                        null
                    }
                }.onFailure { exception ->
                    Logger.e("Failed to register user", exception)
                }

            null
        }
    }
}
