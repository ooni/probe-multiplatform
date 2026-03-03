package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import kotlinx.coroutines.withContext
import org.ooni.engine.WriteResult
import org.ooni.engine.models.Result
import org.ooni.passport.models.CredentialResponse
import org.ooni.passport.models.PassportException
import org.ooni.probe.config.BuildTypeDefaults
import kotlin.coroutines.CoroutineContext

class RegisterUser(
    private val userAuthRegister: (
        String,
        String,
        String,
    ) -> Result<CredentialResponse, PassportException>,
    private val saveCredential: suspend (String, String) -> WriteResult,
    private val backgroundContext: CoroutineContext,
) {
    suspend operator fun invoke(
        publicParams: String,
        manifestVersion: String,
    ): String? {
        return withContext(backgroundContext) {
            val url = "${BuildTypeDefaults.ooniApiBaseUrl}/api/v1/sign_credential"

            userAuthRegister(url, publicParams, manifestVersion)
                .onSuccess { credentialResponse ->
                    if (!credentialResponse.response.isSuccessful) {
                        Logger.w("Failed to register user (status=${credentialResponse.response.statusCode})")
                        return@withContext null
                    }

                    val credential = credentialResponse.credential
                    if (credential.isNullOrEmpty()) {
                        Logger.w("Failed to register user (empty credential)")
                        return@withContext null
                    }

                    try {
                        saveCredential(CredentialsConstants.CREDENTIALS_KEY, credential)
                        Logger.i("User registered successfully, credentials stored")
                        return@withContext credential
                    } catch (e: Exception) {
                        Logger.w("Failed to store credentials", e)
                        return@withContext null
                    }
                }.onFailure { exception ->
                    Logger.w("Failed to register user", exception)
                }

            null
        }
    }
}
