package org.ooni.probe.domain.articles

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Success
import org.ooni.probe.data.models.ArticleModel
import org.ooni.testing.factories.ArticleModelFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock

class RefreshArticlesTest {
    @Test
    fun doNotRefreshOnFailure() =
        runTest {
            var dbCalled = false
            var setPreferenceValue: Any? = null
            val subject = RefreshArticles(
                sources = listOf(RefreshArticles.Source { Failure(Exception()) }),
                refreshArticlesInDatabase = { dbCalled = true },
                getPreference = { flowOf(null) },
                setPreference = { _, value -> setPreferenceValue = value },
            )

            subject()

            assertFalse(dbCalled)
            assertTrue(Clock.System.now().epochSeconds - (setPreferenceValue as Long) <= 1L)
        }

    @Test
    fun doNotRefreshTooSoon() =
        runTest {
            var sourceCalled = false
            val subject = RefreshArticles(
                sources = listOf(
                    RefreshArticles.Source {
                        sourceCalled = true
                        Failure(Exception())
                    },
                ),
                refreshArticlesInDatabase = { },
                getPreference = { flowOf(Clock.System.now().epochSeconds) },
                setPreference = { _, _ -> },
            )

            subject()

            assertFalse(sourceCalled)
        }

    @Test
    fun success() =
        runTest {
            var refreshDbValue: List<ArticleModel>? = null
            var setPreferenceValue: Any? = null
            val articles = listOf(ArticleModelFactory.build())
            val subject = RefreshArticles(
                sources = listOf(RefreshArticles.Source { Success(articles) }),
                refreshArticlesInDatabase = { refreshDbValue = it },
                getPreference = { flowOf(null) },
                setPreference = { _, value -> setPreferenceValue = value },
            )

            subject()

            assertEquals(articles, refreshDbValue)
            assertTrue(Clock.System.now().epochSeconds - (setPreferenceValue as Long) <= 1L)
        }
}
