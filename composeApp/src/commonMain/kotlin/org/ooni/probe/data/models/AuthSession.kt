package org.ooni.probe.data.models

import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * The locally persisted OONI account session used to author OONI Run links.
 *
 * The session token is a Bearer JWT capped at [loginTime] + 10 days server-side regardless of
 * refreshes - once refreshing starts failing, the user needs to log in again.
 */
@Serializable
data class AuthSession(
    val sessionToken: String,
    val emailAddress: String,
    val role: String,
    val loginTime: Instant,
)
