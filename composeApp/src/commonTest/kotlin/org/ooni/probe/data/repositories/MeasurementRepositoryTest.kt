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

class MeasurementRepositoryTest {
    private lateinit var subject: MeasurementRepository

    @BeforeTest
    fun before() {
        subject =
            MeasurementRepository(
                database = Dependencies.buildDatabase(::createTestDatabaseDriver),
                backgroundDispatcher = Dispatchers.Default,
            )
    }

    @Test
    fun createAndGet() =
        runTest {
            val model =
                MeasurementModelFactory.build(id = MeasurementModel.Id(Random.nextLong().absoluteValue))

            subject.create(model)
            val result = subject.list().first().first()

            assertEquals(model, result)
        }
}
