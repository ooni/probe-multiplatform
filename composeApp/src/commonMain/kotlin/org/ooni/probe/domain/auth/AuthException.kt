package org.ooni.probe.domain.auth

import org.ooni.passport.models.PassportException

sealed class AuthException(
    message: String?,
) : Exception(message) {
    data class Http(
        val statusCode: Int,
        val bodyText: String?,
    ) : AuthException("HTTP $statusCode: ${bodyText.orEmpty()}")

    data class Network(
        override val cause: PassportException,
    ) : AuthException(cause.message)

    data class Decode(
        override val cause: Throwable,
    ) : AuthException(cause.message)

    /** No session is stored locally, so there is nothing to refresh or attach as a Bearer token. */
    data object NotLoggedIn : AuthException("Not logged in")
}
