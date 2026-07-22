package org.ooni.probe.domain.articles

import co.touchlab.kermit.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.passport.models.isOfflineFailure
import org.ooni.probe.data.models.ArticleModel
import org.ooni.probe.data.models.ArticlesRefreshState
import org.ooni.probe.data.models.SettingsKey
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class RefreshArticles(
    val hasOoniNews: Boolean,
    val sources: List<Source>,
    val isOnline: () -> Boolean,
    val refreshArticlesInDatabase: suspend (List<ArticleModel>) -> Unit,
    val getPreference: (SettingsKey) -> Flow<Any?>,
    val setPreference: suspend (SettingsKey, Any) -> Unit,
    val updateState: (ArticlesRefreshState) -> Unit = {},
) {
    fun interface Source {
        suspend operator fun invoke(): Result<List<ArticleModel>, Exception>
    }

    /**
     * Guards against two refreshes running at once - app-open/reconnect and a manual pull can
     * otherwise both fire and hit every source twice. `tryLock` rather than `withLock`: the second
     * caller should drop out, not queue up behind the first and then repeat its work.
     */
    private val inFlight = Mutex()

    suspend operator fun invoke(skipIntervalCheck: Boolean = false) {
        if (!hasOoniNews) return

        if (!inFlight.tryLock()) return
        try {
            refresh(skipIntervalCheck)
        } finally {
            inFlight.unlock()
        }
    }

    private suspend fun refresh(skipIntervalCheck: Boolean) {
        if (!isOnline()) {
            // Expected, not a bug: no stamp of any kind, so the refresh runs as soon as a network
            // comes back rather than waiting out an interval it never got to use.
            Logger.i("Skipping article refresh: no active network")
            updateState(ArticlesRefreshState.Failed(offline = true))
            return
        }

        if (!skipIntervalCheck && !isDueForRefresh()) return

        updateState(ArticlesRefreshState.Refreshing)

        val responses = coroutineScope {
            sources.map { async { it() } }.awaitAll()
        }

        responses.forEach { response ->
            response.onFailure { failure ->
                if (failure.isOfflineFailure()) {
                    Logger.i("Skipping article source: no active network")
                } else {
                    Logger.w("Failed to get article source", SourceFailure(failure))
                }
            }
        }

        if (responses.all { it is Success }) {
            refreshArticlesInDatabase(responses.mapNotNull { it.get() }.flatten())
            setPreference(SettingsKey.LAST_ARTICLES_REFRESH, Clock.System.now().epochSeconds)
            updateState(ArticlesRefreshState.Idle)
        } else {
            // Only the attempt is stamped. Stamping the success key here is what used to suppress
            // article refreshes for 24h after a single failed start-up.
            setPreference(
                SettingsKey.LAST_ARTICLES_REFRESH_ATTEMPT,
                Clock.System.now().epochSeconds,
            )
            updateState(
                ArticlesRefreshState.Failed(
                    offline = responses.any { it.getError().isOfflineFailure() },
                ),
            )
        }
    }

    private suspend fun isDueForRefresh(): Boolean {
        val now = Clock.System.now()

        val lastSuccess = getPreference(SettingsKey.LAST_ARTICLES_REFRESH).firstInstant()
        if (lastSuccess != null && now - lastSuccess < MIN_INTERVAL) return false

        val lastAttempt = getPreference(SettingsKey.LAST_ARTICLES_REFRESH_ATTEMPT).firstInstant()
        if (lastAttempt != null && now - lastAttempt < FAILURE_RETRY_INTERVAL) return false

        return true
    }

    private suspend fun Flow<Any?>.firstInstant(): Instant? = (first() as? Long)?.let(Instant::fromEpochSeconds)

    class SourceFailure(
        cause: Throwable,
    ) : Exception(cause)

    companion object {
        private val MIN_INTERVAL = 1.days
        private val FAILURE_RETRY_INTERVAL = 15.minutes
    }
}
