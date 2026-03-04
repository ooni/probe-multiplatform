package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import kotlinx.coroutines.withContext
import org.ooni.passport.PassportAuthRegister
import org.ooni.probe.config.BuildTypeDefaults
import kotlin.coroutines.CoroutineContext

class RegisterUser(
    private val passportAuthRegister: PassportAuthRegister,
    private val setCredential: suspend (String) -> Boolean,
    private val backgroundContext: CoroutineContext,
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

                    val credential = credentialResponse.credential
                    if (credential.isNullOrEmpty()) {
                        Logger.w("Failed to register user (empty credential)")
                        return@withContext null
                    }

                    return@withContext if (setCredential(credential)) {
                        credential
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
