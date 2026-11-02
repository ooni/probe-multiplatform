package org.ooni.probe.data.models

sealed interface SessionState {
    data object Unknown : SessionState

    data class Error(
        val cause: Throwable? = null,
    ) : SessionState

    data object LoggedOut : SessionState

    data class LoggedIn(
        val session: AuthSession,
    ) : SessionState
}
