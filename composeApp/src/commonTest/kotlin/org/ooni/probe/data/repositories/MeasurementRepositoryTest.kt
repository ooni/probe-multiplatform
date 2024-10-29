package org.ooni.probe.data.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.di.Dependencies
import org.ooni.testing.createTestDatabaseDriver
import org.ooni.testing.factories.MeasurementModelFactory
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MeasurementRepositoryTest {
    private lateinit var subject: MeasurementRepository

    @BeforeTest
    fun before() {
        subject =
            MeasurementRepository(
                database = Dependencies.buildDatabase(::createTestDatabaseDriver),
                backgroundContext = Dispatchers.Default,
            )
    }

    @Test
    fun createOrUpdate_withIdAndGet() =
        runTest {
            val model =
                MeasurementModelFactory.build(
                    id = MeasurementModel.Id(Random.nextLong().absoluteValue),
                )

            val modelId = subject.createOrUpdate(model)
            val result = subject.list().first().first()

            assertEquals(model.id, modelId)
            assertEquals(model, result)
        }

    @Test
    fun createOrUpdate_withoutId() =
        runTest {
            val model = MeasurementModelFactory.build(id = null)

            val modelId = subject.createOrUpdate(model)

            assertNotNull(modelId)
        }

    @Test
    fun createAndUpdate() =
        runTest {
            val model = MeasurementModelFactory.build(
                id = MeasurementModel.Id(Random.nextLong().absoluteValue),
                isDone = false,
            )

            subject.createOrUpdate(model)
            with(subject.list().first().first()) {
                assertEquals(model.id, id)
                assertEquals(false, isDone)
            }

            subject.createOrUpdate(model.copy(isDone = true))
            with(subject.list().first().first()) {
                assertEquals(model.id, id)
                assertEquals(true, isDone)
            }
        }
}
