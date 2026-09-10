package org.ooni.probe.domain.auth

import kotlinx.serialization.json.Json
import org.ooni.engine.models.OoniAuthUserSession
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.passport.PassportBridge
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.AuthSession

class RefreshSession(
    private val passportPostWithAuth: suspend (
        url: String,
        payload: String,
        extraHeaders: List<PassportBridge.KeyValue>,
    ) -> Result<PassportHttpResponse, PassportException>,
    private val getStoredSession: suspend () -> AuthSession?,
    private val storeSession: suspend (AuthSession) -> Boolean,
    private val clearSession: suspend () -> Unit,
    private val json: Json,
) {
    suspend operator fun invoke(): Result<AuthSession, AuthException> {
        val current = getStoredSession() ?: return Failure(AuthException.NotLoggedIn)

        val result = passportPostWithAuth(
            "${OrganizationConfig.ooniApiBaseUrl}/api/v2/ooniauth/user-session",
            "",
            bearerAuthHeader(current.sessionToken),
        ).mapError<AuthException> { AuthException.Network(it) }
            .flatMap { decodeAuthResponse<OoniAuthUserSession>(it, json) }
            .flatMap { it.toAuthSessionResult() }

        when {
            result is Success -> storeSession(result.value)
            result is Failure && (result.reason as? AuthException.Http)?.statusCode == 401 -> clearSession()
        }
        return result
    }
}
