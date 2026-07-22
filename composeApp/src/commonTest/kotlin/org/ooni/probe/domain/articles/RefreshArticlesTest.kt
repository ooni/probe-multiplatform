package org.ooni.probe.domain.articles

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Success
import org.ooni.passport.models.PassportException
import org.ooni.probe.data.models.ArticleModel
import org.ooni.probe.data.models.ArticlesRefreshState
import org.ooni.probe.data.models.SettingsKey
import org.ooni.testing.factories.ArticleModelFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class RefreshArticlesTest {
    @Test
    fun doNotRefreshWithNoInternet() =
        runTest {
            var dbCalled = false
            val stamps = mutableMapOf<SettingsKey, Any>()
            var sourceCalled = false
            val states = mutableListOf<ArticlesRefreshState>()
            val subject = subject(
                sources = listOf(
                    RefreshArticles.Source {
                        sourceCalled = true
                        Failure(Exception())
                    },
                ),
                isOnline = false,
                refreshArticlesInDatabase = { dbCalled = true },
                stamps = stamps,
                states = states,
            )

            subject()

            assertFalse(sourceCalled)
            assertFalse(dbCalled)
            // Neither key: an offline start must not consume any backoff window.
            assertTrue(stamps.isEmpty(), "offline refresh must stamp nothing, got $stamps")
            assertEquals(listOf<ArticlesRefreshState>(ArticlesRefreshState.Failed(true)), states)
        }

    /**
     * Deliberately changed: this used to assert the *success* key was stamped on failure, which is
     * exactly the bug - one failed refresh suppressed articles for 24h.
     */
    @Test
    fun failureStampsTheAttemptKeyNotTheSuccessKey() =
        runTest {
            var dbCalled = false
            val stamps = mutableMapOf<SettingsKey, Any>()
            val subject = subject(
                sources = listOf(RefreshArticles.Source { Failure(Exception()) }),
                refreshArticlesInDatabase = { dbCalled = true },
                stamps = stamps,
            )

            subject()

            assertFalse(dbCalled)
            assertNull(stamps[SettingsKey.LAST_ARTICLES_REFRESH])
            val attempt = stamps[SettingsKey.LAST_ARTICLES_REFRESH_ATTEMPT] as Long
            assertTrue(Clock.System.now().epochSeconds - attempt <= 1L)
        }

    @Test
    fun doNotRetryFiveMinutesAfterAFailure() =
        runTest {
            var sourceCalled = false
            val subject = subject(
                sources = listOf(
                    RefreshArticles.Source {
                        sourceCalled = true
                        Failure(Exception())
                    },
                ),
                preferences = mapOf(
                    SettingsKey.LAST_ARTICLES_REFRESH_ATTEMPT to
                        (Clock.System.now() - 5.minutes).epochSeconds,
                ),
            )

            subject()

            assertFalse(sourceCalled, "a failure backs off for 15 minutes")
        }

    @Test
    fun retryFifteenMinutesAfterAFailure() =
        runTest {
            var sourceCalled = false
            val subject = subject(
                sources = listOf(
                    RefreshArticles.Source {
                        sourceCalled = true
                        Failure(Exception())
                    },
                ),
                preferences = mapOf(
                    SettingsKey.LAST_ARTICLES_REFRESH_ATTEMPT to
                        (Clock.System.now() - 16.minutes).epochSeconds,
                ),
            )

            subject()

            assertTrue(sourceCalled)
        }

    @Test
    fun successStillBlocksForTwentyFourHours() =
        runTest {
            var sourceCalled = false
            val subject = subject(
                sources = listOf(
                    RefreshArticles.Source {
                        sourceCalled = true
                        Failure(Exception())
                    },
                ),
                preferences = mapOf(
                    SettingsKey.LAST_ARTICLES_REFRESH to Clock.System.now().epochSeconds,
                ),
            )

            subject()

            assertFalse(sourceCalled)
        }

    @Test
    fun successAfterTwentyFourHoursRefreshesAgain() =
        runTest {
            var sourceCalled = false
            val subject = subject(
                sources = listOf(
                    RefreshArticles.Source {
                        sourceCalled = true
                        Success(emptyList())
                    },
                ),
                preferences = mapOf(
                    SettingsKey.LAST_ARTICLES_REFRESH to
                        (Clock.System.now() - 2.days).epochSeconds,
                ),
            )

            subject()

            assertTrue(sourceCalled)
        }

    @Test
    fun refreshSoonerIfSkip() =
        runTest {
            var sourceCalled = false
            val subject = subject(
                sources = listOf(
                    RefreshArticles.Source {
                        sourceCalled = true
                        Failure(Exception())
                    },
                ),
                preferences = mapOf(
                    SettingsKey.LAST_ARTICLES_REFRESH to Clock.System.now().epochSeconds,
                ),
            )

            subject(skipIntervalCheck = true)

            assertTrue(sourceCalled)
        }

    @Test
    fun success() =
        runTest {
            var refreshDbValue: List<ArticleModel>? = null
            val stamps = mutableMapOf<SettingsKey, Any>()
            val states = mutableListOf<ArticlesRefreshState>()
            val articles = listOf(ArticleModelFactory.build())
            val subject = subject(
                sources = listOf(RefreshArticles.Source { Success(articles) }),
                refreshArticlesInDatabase = { refreshDbValue = it },
                stamps = stamps,
                states = states,
            )

            subject()

            assertEquals(articles, refreshDbValue)
            val stamped = stamps[SettingsKey.LAST_ARTICLES_REFRESH] as Long
            assertTrue(Clock.System.now().epochSeconds - stamped <= 1L)
            assertNull(stamps[SettingsKey.LAST_ARTICLES_REFRESH_ATTEMPT])
            assertEquals(
                listOf(ArticlesRefreshState.Refreshing, ArticlesRefreshState.Idle),
                states,
            )
        }

    @Test
    fun offlineSourceFailureIsReportedAsOffline() =
        runTest {
            val states = mutableListOf<ArticlesRefreshState>()
            val subject = subject(
                sources = listOf(
                    RefreshArticles.Source { Failure(PassportException.Offline("no network")) },
                ),
                states = states,
            )

            subject()

            assertEquals(ArticlesRefreshState.Failed(offline = true), states.last())
        }

    /**
     * App-open/reconnect and a manual pull-to-refresh can fire together; each source must still be
     * hit only once.
     */
    @Test
    fun concurrentInvocationsCallEachSourceOnce() =
        runTest {
            val release = CompletableDeferred<Unit>()
            var sourceCalls = 0
            val subject = subject(
                sources = listOf(
                    RefreshArticles.Source {
                        sourceCalls++
                        release.await()
                        Success(emptyList())
                    },
                ),
            )

            val first = async { subject() }
            val second = async { subject(skipIntervalCheck = true) }

            release.complete(Unit)
            first.await()
            second.await()

            assertEquals(1, sourceCalls)
        }

    @Test
    fun doNothingWithoutOoniNews() =
        runTest {
            var sourceCalled = false
            val states = mutableListOf<ArticlesRefreshState>()
            val subject = subject(
                hasOoniNews = false,
                sources = listOf(
                    RefreshArticles.Source {
                        sourceCalled = true
                        Success(emptyList())
                    },
                ),
                states = states,
            )

            subject()

            assertFalse(sourceCalled)
            assertTrue(states.isEmpty())
        }

    private fun subject(
        sources: List<RefreshArticles.Source>,
        hasOoniNews: Boolean = true,
        isOnline: Boolean = true,
        refreshArticlesInDatabase: suspend (List<ArticleModel>) -> Unit = {},
        preferences: Map<SettingsKey, Any?> = emptyMap(),
        stamps: MutableMap<SettingsKey, Any> = mutableMapOf(),
        states: MutableList<ArticlesRefreshState> = mutableListOf(),
    ) = RefreshArticles(
        hasOoniNews = hasOoniNews,
        sources = sources,
        isOnline = { isOnline },
        refreshArticlesInDatabase = refreshArticlesInDatabase,
        getPreference = { key -> flowOf(stamps[key] ?: preferences[key]) },
        setPreference = { key, value -> stamps[key] = value },
        updateState = { states += it },
    )
}
