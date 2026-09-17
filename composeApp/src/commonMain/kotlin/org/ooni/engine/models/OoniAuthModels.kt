package org.ooni.engine.models

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire models for the `ooniauth` service's passwordless "magic link" flow.
 *
 * @see [https://github.com/ooni/backend/blob/master/ooniapi/services/ooniauth/src/ooniauth/routers/v2.py]
 */
@Serializable
data class OoniAuthLoginRequest(
    @SerialName("email_address") val emailAddress: String,
    @SerialName("redirect_to") val redirectTo: String,
)

@Serializable
data class OoniAuthLoginResponse(
    @SerialName("email_address") val emailAddress: String,
    @SerialName("login_token_expiration") val loginTokenExpiration: Instant,
)

@Serializable
data class OoniAuthSessionRequest(
    @SerialName("login_token") val loginToken: String,
)

/**
 * Response of both the login-token exchange and the no-body session refresh/check calls.
 * Fields other than [isLoggedIn] are null when there is no valid session.
 */
@Serializable
data class OoniAuthUserSession(
    @SerialName("session_token") val sessionToken: String? = null,
    @SerialName("redirect_to") val redirectTo: String? = null,
    @SerialName("email_address") val emailAddress: String? = null,
    @SerialName("account_id") val accountId: String? = null,
    @SerialName("role") val role: String? = null,
    @SerialName("login_time") val loginTime: Instant? = null,
    @SerialName("is_logged_in") val isLoggedIn: Boolean = false,
)
