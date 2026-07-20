package org.ooni.passport

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.ooni.engine.models.Result
import org.ooni.passport.models.CredentialResponse
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.passport.models.SubmitCredentialConfig
import org.ooni.probe.data.models.ProxyOption
import org.ooni.probe.domain.credentials.CredentialsConstants

/**
 * A wrapper around PassportBridge that handles proxy resolution, timeout, and empty headers/query by default.
 *
 * Domain classes depend on this wrapper instead of raw PassportGet/PassportPost/etc. interfaces.
 * This eliminates the need for each domain class to:
 * - Resolve proxy from getProxyOption
 * - Pass empty headers/query
 * - Pass the timeout constant
 */
class PassportHttpClient(
    @get:VisibleForTesting
    var passportGet: PassportGet,
    private val passportPost: PassportPost,
    private val passportAuthRegister: PassportAuthRegister,
    private val passportAuthSubmit: PassportAuthSubmit,
    private val getProxyOption: () -> Flow<ProxyOption>,
    private val timeout: Float = CredentialsConstants.HTTP_TIMEOUT_SECONDS,
) {
    private suspend fun resolveProxy(): String? = getProxyOption().first().value.takeIf { it.isNotEmpty() }

    suspend fun get(
        url: String,
        proxyOverride: String? = null,
    ): Result<PassportHttpResponse, PassportException> {
        val proxy = proxyOverride ?: resolveProxy()
        return passportGet.get(
            url = url,
            headers = emptyList(),
            query = emptyList(),
            proxy = proxy,
            timeout = timeout,
        )
    }

    suspend fun post(
        url: String,
        payload: String,
    ): Result<PassportHttpResponse, PassportException> {
        val proxy = resolveProxy()
        return passportPost.post(
            url = url,
            headers = emptyList(),
            payload = payload,
            proxy = proxy,
            timeout = timeout,
        )
    }

    suspend fun userAuthRegister(
        url: String,
        publicParams: String,
        manifestVersion: String,
    ): Result<CredentialResponse, PassportException> {
        val proxy = resolveProxy()
        return passportAuthRegister.userAuthRegister(
            url = url,
            publicParams = publicParams,
            manifestVersion = manifestVersion,
            proxy = proxy,
            timeout = timeout,
        )
    }

    suspend fun userAuthSubmit(
        url: String,
        content: String,
        probeCc: String,
        probeAsn: String,
        credentialConfig: SubmitCredentialConfig?,
    ): Result<CredentialResponse, PassportException> {
        val proxy = resolveProxy()
        return passportAuthSubmit.userAuthSubmit(
            url = url,
            content = content,
            probeCc = probeCc,
            probeAsn = probeAsn,
            proxy = proxy,
            timeout = timeout,
            credentialConfig = credentialConfig,
        )
    }
}
