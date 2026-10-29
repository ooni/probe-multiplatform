package org.ooni.probe.update

import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Decides which appcast feed to check, when to retry a failed feed and when to move on to
 * the next one. It does not talk to the updater: every check goes through [check], which
 * points the updater to the given feed and returns how the check ended.
 *
 * Keeps the current feed as plain state, so calls must not overlap;
 * [DesktopUpdateController] runs them one at a time.
 */
class UpdateFeedFailover(
    private val urls: List<String>,
    private val check: suspend (url: String, showUI: Boolean) -> CheckOutcome,
    private val loadSavedIndex: suspend () -> Int?,
    private val saveIndex: suspend (Int) -> Unit,
    private val onFailover: suspend (from: String, to: String, showUI: Boolean) -> Unit = { _, _, _ -> },
    private val retryDelays: List<Duration> = DEFAULT_RETRY_DELAYS,
    private val switchDelay: Duration = DEFAULT_SWITCH_DELAY,
) {
    init {
        require(urls.isNotEmpty()) { "At least one feed URL is required" }
    }

    private var currentIndex = 0
    private var savedIndex: Int? = null

    /** Feed the next check starts with. */
    val currentUrl: String get() = urls[currentIndex]

    /** Loads the last feed that worked, falling back to the primary one. */
    suspend fun loadPreferredUrl(): String {
        savedIndex = loadSavedIndex()?.takeIf { it in urls.indices }
        currentIndex = savedIndex ?: 0
        return currentUrl
    }

    /**
     * Check without UI. A failing feed is retried with backoff before moving on to the next.
     */
    suspend fun checkInBackground(): CheckOutcome {
        val outcome = checkFeeds(feedOrder(), showUI = false, retryDelays = retryDelays)
        if (!outcome.feedWorked) {
            Logger.w("Update check failed ($outcome); waiting for the next scheduled check")
            resetToPrimaryFeed()
        }
        return outcome
    }

    /**
     * Check requested by the user, with UI.
     *
     * With [canDeferErrors], the updater holds back its error dialog, so every feed is checked
     * with UI and the caller reports the error once all of them failed. Otherwise the updater
     * already showed its error for the current feed, and the remaining feeds are checked in the
     * background so that later checks use the one that works.
     */
    suspend fun checkByUser(canDeferErrors: Boolean): CheckOutcome {
        val feeds = feedOrder()
        val outcome = if (canDeferErrors) {
            checkFeeds(feeds, showUI = true, retryDelays = emptyList())
        } else {
            val userOutcome = checkFeeds(feeds.take(1), showUI = true, retryDelays = emptyList())
            if (userOutcome != CheckOutcome.FeedFailure || feeds.size == 1) {
                userOutcome
            } else {
                delay(switchDelay)
                checkFeeds(feeds.drop(1), showUI = false, retryDelays = retryDelays)
            }
        }
        if (!outcome.feedWorked) {
            Logger.w("User-initiated update check failed ($outcome)")
            resetToPrimaryFeed()
        }
        return outcome
    }

    private suspend fun checkFeeds(
        feeds: List<Int>,
        showUI: Boolean,
        retryDelays: List<Duration>,
    ): CheckOutcome {
        var outcome: CheckOutcome = CheckOutcome.FeedFailure
        feeds.forEachIndexed { position, index ->
            // A check started right after an abort is swallowed by the native updater
            if (position > 0) {
                delay(switchDelay)
                Logger.i("Failing over update feed from ${urls[currentIndex]} to ${urls[index]}")
                onFailover(urls[currentIndex], urls[index], showUI)
            }
            currentIndex = index

            outcome = check(urls[index], showUI)
            for ((attempt, retryDelay) in retryDelays.withIndex()) {
                if (outcome != CheckOutcome.FeedFailure) break
                Logger.i("Retrying update check ${attempt + 1}/${retryDelays.size} in $retryDelay")
                delay(retryDelay)
                outcome = check(urls[index], showUI)
            }

            if (outcome != CheckOutcome.FeedFailure) {
                if (outcome.feedWorked) rememberWorkingFeed(index)
                return outcome
            }
            Logger.i("Update feed ${urls[index]} failed")
        }
        return outcome
    }

    /** Feed indices starting with the current one. */
    private fun feedOrder() = urls.indices.map { (currentIndex + it) % urls.size }

    private suspend fun rememberWorkingFeed(index: Int) {
        if (savedIndex != index) {
            savedIndex = index
            saveIndex(index)
        }
    }

    private fun resetToPrimaryFeed() {
        currentIndex = 0
    }

    companion object {
        val DEFAULT_RETRY_DELAYS = listOf(1.minutes, 5.minutes, 15.minutes)
        val DEFAULT_SWITCH_DELAY = 5.seconds
    }
}
