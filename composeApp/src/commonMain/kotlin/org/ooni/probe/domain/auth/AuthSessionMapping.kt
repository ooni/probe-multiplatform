package org.ooni.probe.domain.auth

import org.ooni.engine.models.Failure
import org.ooni.engine.models.OoniAuthUserSession
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.probe.data.models.AuthSession

internal fun OoniAuthUserSession.toAuthSessionResult(): Result<AuthSession, AuthException> {
    val token = sessionToken
    val email = emailAddress
    val roleValue = role
    val time = loginTime
    return if (isLoggedIn && token != null && email != null && roleValue != null && time != null) {
        Success(AuthSession(sessionToken = token, emailAddress = email, role = roleValue, loginTime = time))
    } else {
        Failure(AuthException.Decode(IllegalStateException("Incomplete or logged-out session in response")))
    }
}
