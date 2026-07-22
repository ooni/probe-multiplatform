package org.ooni.probe.data.models

/**
 * Shared state of the article refresh, so the dashboard and the articles screen can tell
 * "loading with an empty cache" apart from "nothing to show".
 */
sealed interface ArticlesRefreshState {
    data object Idle : ArticlesRefreshState

    data object Refreshing : ArticlesRefreshState

    data class Failed(
        val offline: Boolean,
    ) : ArticlesRefreshState

    val isRefreshing get() = this is Refreshing
}
