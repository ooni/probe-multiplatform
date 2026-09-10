package org.ooni.probe.domain.auth

import kotlinx.serialization.json.Json
import org.ooni.engine.models.OoniAuthSessionRequest
import org.ooni.engine.models.OoniAuthUserSession
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.AuthSession

class ExchangeLoginToken(
    private val passportPost: suspend (url: String, payload: String) -> Result<PassportHttpResponse, PassportException>,
    private val storeSession: suspend (AuthSession) -> Boolean,
    private val json: Json,
) {
    suspend operator fun invoke(loginToken: String): Result<AuthSession, AuthException> {
        val body = json.encodeToString(OoniAuthSessionRequest.serializer(), OoniAuthSessionRequest(loginToken))
        val result = passportPost("${OrganizationConfig.ooniApiBaseUrl}/api/v2/ooniauth/user-session", body)
            .mapError<AuthException> { AuthException.Network(it) }
            .flatMap { decodeAuthResponse<OoniAuthUserSession>(it, json) }
            .flatMap { it.toAuthSessionResult() }

        if (result is Success) {
            storeSession(result.value)
        }
        return result
    }
}
