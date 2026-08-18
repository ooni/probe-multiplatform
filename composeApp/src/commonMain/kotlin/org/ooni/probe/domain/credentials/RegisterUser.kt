package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.passport.models.CredentialResponse
import org.ooni.passport.models.PassportException
import org.ooni.probe.config.BuildTypeDefaults
import org.ooni.probe.data.models.Credential
import org.ooni.probe.data.models.Manifest
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
    private val retrieveManifest: suspend () -> Manifest? = { null },
) {
    suspend operator fun invoke(
        publicParams: String,
        manifestVersion: String,
    ): Credential? = withContext(backgroundContext) { register(publicParams, manifestVersion) }

    private suspend fun register(
        publicParams: String,
        manifestVersion: String,
        canRefreshManifest: Boolean = true,
    ): Credential? {
        val url = "${BuildTypeDefaults.ooniApiBaseUrl}/api/v1/sign_credential"

        return when (val result = userAuthRegister(url, publicParams, manifestVersion)) {
            is Success -> {
                val credentialResponse = result.value
                if (!credentialResponse.response.isSuccessful) {
                    if (credentialResponse.response.statusCode == HTTP_NOT_FOUND && canRefreshManifest) {
                        val manifest = retrieveManifest()
                        if (manifest != null && manifest.meta.version != manifestVersion) {
                            return register(
                                publicParams = manifest.manifest.publicParameters,
                                manifestVersion = manifest.meta.version,
                                canRefreshManifest = false,
                            )
                        }
                    }
                    Logger.w("Failed to register user (status=${credentialResponse.response.statusCode})")
                    null
                } else {
                    val credential = credentialResponse.decodeCredential(json)
                    if (credential == null) {
                        Logger.i("Failed to register user (could not decode credential)")
                        null
                    } else if (setCredential(credential)) {
                        credential
                    } else {
                        Logger.i("Failed to register user: could not store credential in secure storage")
                        null
                    }
                }
            }

            is Failure -> {
                Logger.e("Failed to register user", result.reason)
                null
            }
        }
    }

    private companion object {
        /**
         * Backend returns `404` when the supplied anonymous-credentials manifest version is stale.
         * See <https://github.com/ooni/backend/blob/e5aa51a275622dec67aea763c41f7eded33cbcbd/ooniapi/services/ooniprobe/src/ooniprobe/routers/v1/probe_services.py#L765-L785>.
         */
        const val HTTP_NOT_FOUND = 404
    }
}
