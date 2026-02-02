package org.ooni.probe.data.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.ooni.probe.di.Dependencies
import org.ooni.testing.createTestDatabaseDriver
import org.ooni.testing.factories.ArticleModelFactory
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArticleRepositoryTest {
    private lateinit var subject: ArticleRepository

    @BeforeTest
    fun before() {
        subject = ArticleRepository(
            database = Dependencies.buildDatabase(::createTestDatabaseDriver),
            backgroundContext = Dispatchers.Default,
        )
    }

    @Test
    fun refreshAndList() =
        runTest {
            val articleToRemove = ArticleModelFactory.build()
            val articleToKeep = ArticleModelFactory.build()
            val articleToAdd = ArticleModelFactory.build()
            subject.refresh(listOf(articleToRemove, articleToKeep))
            subject.refresh(listOf(articleToKeep, articleToAdd))

            val result = subject.list().first()

            assertEquals(2, result.size)
            assertFalse(result.contains(articleToRemove))
            assertTrue(result.contains(articleToKeep))
            assertTrue(result.contains(articleToAdd))
        }
}
