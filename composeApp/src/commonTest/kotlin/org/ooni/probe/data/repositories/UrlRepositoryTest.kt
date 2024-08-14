package org.ooni.probe.data.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.WebConnectivityCategory
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
            val model =
                UrlModelFactory.build(
                    id = null,
                    countryCode = "IT",
                    category = WebConnectivityCategory.ENV,
                )

            subject.createOrUpdate(model)
            val result = subject.list().first().first()

            assertNotNull(result.id)
            assertEquals(model.url, result.url)
            assertEquals(model.countryCode, result.countryCode)
            assertEquals(model.category, result.category)
        }

    @Test
    fun createWithIdAndGet() =
        runTest {
            val model = UrlModelFactory.build(id = UrlModel.Id(Random.nextLong().absoluteValue))

            val modelId = subject.createOrUpdate(model)
            val result = subject.list().first().first()

            assertEquals(model.id, modelId)
            assertEquals(model, result)
        }

    @Test
    fun createOrUpdateByUrls() =
        runTest {
            val existingModel =
                UrlModelFactory.build(
                    id = UrlModel.Id(Random.nextLong().absoluteValue),
                    url = "https://example.org",
                    countryCode = null,
                    category = null,
                )
            subject.createOrUpdate(existingModel)

            val modelWithSameUrl =
                UrlModelFactory.build(
                    id = null,
                    url = existingModel.url,
                    countryCode = "US",
                    category = WebConnectivityCategory.ENV,
                )
            val modelWithNewUrl =
                UrlModelFactory.build(
                    id = null,
                    url = "https://ooni.org",
                    countryCode = "IT",
                    category = WebConnectivityCategory.NEWS,
                )

            val results = subject.createOrUpdateByUrl(listOf(modelWithSameUrl, modelWithNewUrl))
            val allUrls = subject.list().first()

            assertEquals(2, allUrls.size)

            with(results.first()) {
                assertEquals(existingModel.id, id)
                assertEquals(existingModel.url, url)
                assertEquals(modelWithSameUrl.category, category)
                assertEquals(modelWithSameUrl.countryCode, countryCode)
            }
            with(results.last()) {
                assertNotNull(id)
                assertEquals(modelWithNewUrl.url, url)
                assertEquals(modelWithNewUrl.category, category)
                assertEquals(modelWithNewUrl.countryCode, countryCode)
            }
        }

    @Test
    fun getByUrl() =
        runTest {
            val url = "htts://example.org"
            val model = UrlModelFactory.build(url = url)

            subject.createOrUpdate(model)
            val result = subject.getByUrl(url).first()

            assertNotNull(result)
            assertEquals(url, result.url)
        }
}
