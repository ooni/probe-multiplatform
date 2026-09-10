package org.ooni.probe.domain.auth

import kotlinx.serialization.json.Json
import org.ooni.engine.models.OoniAuthLoginRequest
import org.ooni.engine.models.OoniAuthLoginResponse
import org.ooni.engine.models.Result
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.probe.config.OrganizationConfig

class RequestLogin(
    private val passportPost: suspend (url: String, payload: String) -> Result<PassportHttpResponse, PassportException>,
    private val json: Json,
) {
    suspend operator fun invoke(emailAddress: String): Result<OoniAuthLoginResponse, AuthException> {
        val request = OoniAuthLoginRequest(
            emailAddress = emailAddress,
            redirectTo = "${OrganizationConfig.ooniRunDashboardUrl}/login",
        )
        val body = json.encodeToString(OoniAuthLoginRequest.serializer(), request)
        return passportPost("${OrganizationConfig.ooniApiBaseUrl}/api/v2/ooniauth/user-login", body)
            .mapError<AuthException> { AuthException.Network(it) }
            .flatMap { decodeAuthResponse<OoniAuthLoginResponse>(it, json) }
    }
}
