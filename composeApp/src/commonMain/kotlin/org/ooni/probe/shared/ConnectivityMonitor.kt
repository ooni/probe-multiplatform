package org.ooni.probe.shared

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.ooni.engine.NetworkTypeFinder
import org.ooni.engine.models.NetworkType
import kotlin.time.Duration.Companion.seconds

/**
 * Single source of truth for "does this device currently have a usable network?".
 */
class ConnectivityMonitor(
    private val networkTypeFinder: NetworkTypeFinder,
) {
    fun isOnline(): Boolean = networkTypeFinder() != NetworkType.NoInternet

    /**
     * Emits the current state immediately, then only on change. Polls rather than registering a
     * platform callback so that a single implementation covers Android, iOS and desktop.
     */
    fun observeIsOnline(): Flow<Boolean> =
        tickerFlow(POLL_INTERVAL)
            .map { isOnline() }
            .distinctUntilChanged()

    companion object {
        @VisibleForTesting
        val POLL_INTERVAL = 5.seconds
    }
}
