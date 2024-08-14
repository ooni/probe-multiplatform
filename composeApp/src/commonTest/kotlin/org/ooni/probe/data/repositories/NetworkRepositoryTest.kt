package org.ooni.probe.data.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.ooni.probe.data.models.NetworkModel
import org.ooni.probe.di.Dependencies
import org.ooni.testing.createTestDatabaseDriver
import org.ooni.testing.factories.NetworkModelFactory
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkRepositoryTest {
    private lateinit var subject: NetworkRepository

    @BeforeTest
    fun before() {
        subject =
            NetworkRepository(
                database = Dependencies.buildDatabase(::createTestDatabaseDriver),
                backgroundDispatcher = Dispatchers.Default,
            )
    }

    @Test
    fun createIf_WithIdOnlyUpdates() =
        runTest {
            val model = NetworkModelFactory.build(id = NetworkModel.Id(Random.nextLong().absoluteValue))

            subject.createIfNew(model)
            val result = subject.list().first().first()

            assertEquals(model, result)
        }

    @Test
    fun createIfNew_WithSameValuesDoesNotCreate() =
        runTest {
            val model1 = NetworkModelFactory.build(id = NetworkModel.Id(Random.nextLong().absoluteValue))
            val model2 = model1.copy(id = null)

            subject.createIfNew(model1)
            val modelId2 = subject.createIfNew(model2)

            assertEquals(modelId2, model1.id)
        }
}
