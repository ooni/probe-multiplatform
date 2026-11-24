package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.ooni.probe.data.models.SettingsKey
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/**
 * Android gives dataSync foreground services 6 hours per 24 hours. When we hit that, workers
 * stay off until the 24 hours pass or the app is opened.
 *
 * See https://developer.android.com/develop/background-work/services/fgs/timeout
 */
class BackgroundWorkLimit(
    private val getPreference: (SettingsKey) -> Flow<Any?>,
    private val setPreference: suspend (SettingsKey, Any?) -> Unit,
    private val removePreference: suspend (SettingsKey) -> Unit,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    // Visible before the DataStore write finishes
    @Volatile
    private var inMemoryReachedAt: Long? = null

    suspend fun isExhausted(): Boolean {
        val reachedAt = inMemoryReachedAt
            ?: getPreference(KEY).first() as? Long
            ?: return false
        // Clock went back: don't block forever
        return (now() - reachedAt) in 0 until WINDOW.inWholeMilliseconds
    }

    // The system restarts the stopped worker right away, so it must see this immediately
    suspend fun markReached() {
        val reachedAt = now()
        inMemoryReachedAt = reachedAt
        setPreference(KEY, reachedAt)
    }

    suspend fun reset() {
        inMemoryReachedAt = null
        removePreference(KEY)
    }

    companion object {
        private val KEY = SettingsKey.BACKGROUND_WORK_LIMIT_REACHED_AT
        private val WINDOW = 24.hours
    }
}
