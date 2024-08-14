package org.ooni.probe.data.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.ooni.probe.data.models.UrlModel
import org.ooni.probe.di.Dependencies
import org.ooni.testing.createTestDatabaseDriver
import org.ooni.testing.factories.UrlModelFactory
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UrlRepositoryTest {
    private lateinit var subject: UrlRepository

    @BeforeTest
    fun before() {
        subject =
            UrlRepository(
                database = Dependencies.buildDatabase(::createTestDatabaseDriver),
                backgroundDispatcher = Dispatchers.Default,
            )
    }

    @Test
    fun createWithoutIdAndGet() =
        runTest {
            val model = UrlModelFactory.build(id = null)

            subject.create(model)
            val result = subject.list().first().first()

            assertNotNull(result.id)
            assertEquals(model.url, result.url)
            assertEquals(model.countryCode, result.countryCode)
            assertEquals(model.categoryCode, result.categoryCode)
        }

    @Test
    fun createWithIdAndGet() =
        runTest {
            val model = UrlModelFactory.build(id = UrlModel.Id(Random.nextLong().absoluteValue))

            subject.create(model)
            val result = subject.list().first().first()

            assertEquals(model, result)
        }
}
