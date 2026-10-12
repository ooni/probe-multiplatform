package org.ooni.probe.update

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.ooni.probe.shared.UpdateError
import org.ooni.probe.update.CheckOutcome.FeedFailure
import org.ooni.probe.update.CheckOutcome.NoUpdate
import org.ooni.probe.update.CheckOutcome.OtherError
import org.ooni.probe.update.CheckOutcome.UpdateAvailable
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateFeedFailoverTest {
    private val primary = "https://primary/feed.rss"
    private val mirror = "https://mirror/feed.rss"

    private data class Check(
        val url: String,
        val showUI: Boolean,
        val time: Long,
    )

    private class Fixture(
        testScope: TestScope,
        urls: List<String>,
        savedIndex: Int?,
        private val outcomes: Map<String, CheckOutcome>,
    ) {
        val checks = mutableListOf<Check>()
        val saved = mutableListOf<Int>()

        val subject = UpdateFeedFailover(
            urls = urls,
            check = { url, showUI ->
                checks += Check(url, showUI, testScope.currentTime)
                outcomes.getValue(url)
            },
            loadSavedIndex = { savedIndex },
            saveIndex = { saved += it },
            retryDelays = listOf(100.milliseconds, 200.milliseconds, 300.milliseconds),
            switchDelay = 10.milliseconds,
        )
    }

    private fun TestScope.fixture(
        outcomes: Map<String, CheckOutcome>,
        urls: List<String> = listOf(primary, mirror),
        savedIndex: Int? = null,
    ) = Fixture(this, urls, savedIndex, outcomes)

    @Test
    fun startsWithPrimaryWhenNothingSaved() =
        runTest {
            val f = fixture(emptyMap())
            assertEquals(primary, f.subject.loadPreferredUrl())
        }

    @Test
    fun startsWithSavedFeed() =
        runTest {
            val f = fixture(emptyMap(), savedIndex = 1)
            assertEquals(mirror, f.subject.loadPreferredUrl())
        }

    @Test
    fun ignoresOutOfRangeSavedIndex() =
        runTest {
            val f = fixture(emptyMap(), savedIndex = 5)
            assertEquals(primary, f.subject.loadPreferredUrl())
        }

    @Test
    fun backgroundSuccessOnPrimaryKeepsIt() =
        runTest {
            val f = fixture(mapOf(primary to NoUpdate))
            f.subject.loadPreferredUrl()

            assertEquals(NoUpdate, f.subject.checkInBackground())

            assertEquals(listOf(Check(primary, false, 0)), f.checks)
            assertEquals(listOf(0), f.saved)
            assertEquals(primary, f.subject.currentUrl)
        }

    @Test
    fun backgroundRetriesWithBackoffThenSwitchesToMirror() =
        runTest {
            val f = fixture(mapOf(primary to FeedFailure, mirror to UpdateAvailable))
            f.subject.loadPreferredUrl()

            assertEquals(UpdateAvailable, f.subject.checkInBackground())

            assertEquals(
                listOf(
                    Check(primary, false, 0),
                    Check(primary, false, 100),
                    Check(primary, false, 300),
                    Check(primary, false, 600),
                    Check(mirror, false, 610),
                ),
                f.checks,
            )
            assertEquals(listOf(1), f.saved)
            assertEquals(mirror, f.subject.currentUrl)
        }

    @Test
    fun backgroundStartsWithSavedFeed() =
        runTest {
            val f = fixture(mapOf(mirror to NoUpdate), savedIndex = 1)
            f.subject.loadPreferredUrl()

            f.subject.checkInBackground()

            assertEquals(listOf(Check(mirror, false, 0)), f.checks)
            assertEquals(emptyList(), f.saved)
        }

    @Test
    fun backgroundResetsToPrimaryWhenAllFeedsFail() =
        runTest {
            val f = fixture(mapOf(primary to FeedFailure, mirror to FeedFailure), savedIndex = 1)
            f.subject.loadPreferredUrl()

            assertEquals(FeedFailure, f.subject.checkInBackground())

            assertEquals(8, f.checks.size)
            assertEquals(listOf(mirror, primary), f.checks.map { it.url }.distinct())
            assertEquals(emptyList(), f.saved)
            assertEquals(primary, f.subject.currentUrl)
        }

    @Test
    fun otherErrorDoesNotFailOver() =
        runTest {
            val f = fixture(mapOf(primary to OtherError))
            f.subject.loadPreferredUrl()

            assertEquals(OtherError, f.subject.checkInBackground())

            assertEquals(listOf(Check(primary, false, 0)), f.checks)
            assertEquals(emptyList(), f.saved)
        }

    @Test
    fun otherErrorOnMirrorResetsToPrimary() =
        runTest {
            val f = fixture(mapOf(primary to FeedFailure, mirror to OtherError))
            f.subject.loadPreferredUrl()

            assertEquals(OtherError, f.subject.checkByUser(canDeferErrors = true))

            assertEquals(primary, f.subject.currentUrl)
            assertEquals(emptyList(), f.saved)
        }

    @Test
    fun userCheckOnWorkingPrimaryChecksOnce() =
        runTest {
            val f = fixture(mapOf(primary to NoUpdate))
            f.subject.loadPreferredUrl()

            assertEquals(NoUpdate, f.subject.checkByUser(canDeferErrors = true))

            assertEquals(listOf(Check(primary, true, 0)), f.checks)
        }

    @Test
    fun userCheckTriesMirrorWithUiWithoutRetries() =
        runTest {
            val f = fixture(mapOf(primary to FeedFailure, mirror to NoUpdate))
            f.subject.loadPreferredUrl()

            assertEquals(NoUpdate, f.subject.checkByUser(canDeferErrors = true))

            assertEquals(listOf(Check(primary, true, 0), Check(mirror, true, 10)), f.checks)
            assertEquals(listOf(1), f.saved)
            assertEquals(mirror, f.subject.currentUrl)
        }

    @Test
    fun userCheckTriesEveryFeedOnceWhenAllFail() =
        runTest {
            val f = fixture(mapOf(primary to FeedFailure, mirror to FeedFailure))
            f.subject.loadPreferredUrl()

            assertEquals(FeedFailure, f.subject.checkByUser(canDeferErrors = true))

            assertEquals(listOf(Check(primary, true, 0), Check(mirror, true, 10)), f.checks)
            assertEquals(primary, f.subject.currentUrl)
        }

    @Test
    fun userCheckWithoutDeferredErrorsFallsBackToBackgroundForOtherFeeds() =
        runTest {
            val f = fixture(mapOf(primary to FeedFailure, mirror to FeedFailure))
            f.subject.loadPreferredUrl()

            assertEquals(FeedFailure, f.subject.checkByUser(canDeferErrors = false))

            assertEquals(
                listOf(
                    Check(primary, true, 0),
                    Check(mirror, false, 10),
                    Check(mirror, false, 110),
                    Check(mirror, false, 310),
                    Check(mirror, false, 610),
                ),
                f.checks,
            )
            assertEquals(primary, f.subject.currentUrl)
        }

    @Test
    fun cancellingCheckStopsFurtherAttempts() =
        runTest {
            val f = fixture(mapOf(primary to FeedFailure, mirror to NoUpdate))
            f.subject.loadPreferredUrl()

            val job = launch { f.subject.checkInBackground() }
            advanceTimeBy(150)
            job.cancel()
            runCurrent()
            advanceTimeBy(10_000)

            assertEquals(listOf(Check(primary, false, 0), Check(primary, false, 100)), f.checks)
            assertEquals(emptyList(), f.saved)
        }

    @Test
    fun checkOutcomeFromError() {
        fun outcome(code: Int) = CheckOutcome.fromError(UpdateError(code, "", "updateCheck"))

        assertEquals(FeedFailure, outcome(-1009)) // NSURLErrorNotConnectedToInternet
        assertEquals(FeedFailure, outcome(-1)) // network error, WinSparkle failure
        assertEquals(FeedFailure, outcome(1000))
        assertEquals(FeedFailure, outcome(1002))
        assertEquals(FeedFailure, outcome(2001))
        assertEquals(OtherError, outcome(3001)) // SUSignatureError
        assertEquals(OtherError, outcome(4005)) // SUInstallationError
        assertEquals(OtherError, outcome(-2)) // bridge exception
        assertEquals(OtherError, outcome(-3)) // invalid public key
        assertEquals(OtherError, outcome(-4)) // invalid public key length
        assertEquals(OtherError, outcome(-999)) // setup error
    }
}
