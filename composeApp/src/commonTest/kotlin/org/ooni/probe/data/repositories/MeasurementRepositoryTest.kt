package org.ooni.probe.data.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.di.Dependencies
import org.ooni.testing.createTestDatabaseDriver
import org.ooni.testing.factories.DescriptorFactory
import org.ooni.testing.factories.MeasurementModelFactory
import org.ooni.testing.factories.ResultModelFactory
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MeasurementRepositoryTest {
    private lateinit var subject: MeasurementRepository
    private lateinit var resultRepository: ResultRepository
    private val json = Dependencies.buildJson()

    @BeforeTest
    fun before() {
        val database = Dependencies.buildDatabase(::createTestDatabaseDriver)
        subject = MeasurementRepository(
            database = database,
            backgroundContext = Dispatchers.Default,
            json = json,
        )
        resultRepository = ResultRepository(
            database = database,
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

    @Test
    fun createAndDelete() =
        runTest {
            val model = MeasurementModelFactory.build(id = null)
            val modelId = subject.createOrUpdate(model)

            assertEquals(1, subject.list().first().size)

            subject.deleteById(modelId)

            assertEquals(0, subject.list().first().size)
        }

    @Test
    fun selectWithoutResult() =
        runTest {
            val resultId = resultRepository.createOrUpdate(ResultModelFactory.build())
            val modelWithResult = MeasurementModelFactory.build(
                id = MeasurementModel.Id(1L),
                resultId = resultId,
            )
            subject.createOrUpdate(modelWithResult)
            val modelWithoutResult = MeasurementModelFactory.build(
                id = MeasurementModel.Id(2L),
                resultId = ResultModel.Id(9L),
            )
            subject.createOrUpdate(modelWithoutResult)

            val output = subject.listWithoutResult().first()

            assertEquals(1, output.size)
            assertEquals(modelWithoutResult.id, output.first().id)
        }

    @Test
    fun selectTestKeys() =
        runTest {
            val descriptor = DescriptorFactory.buildDescriptorWithInstalled()
            val installedDescriptor = (descriptor.source as Descriptor.Source.Installed).value
            val resultId1 = resultRepository.createOrUpdate(
                ResultModelFactory.build(id = null, descriptorKey = installedDescriptor.key),
            )
            val resultId2 = resultRepository.createOrUpdate(
                ResultModelFactory.build(id = null, descriptorName = "circumvention"),
            )
            val model1 = MeasurementModelFactory.build(
                resultId = resultId1,
                testKeys = "{\"blocking\":\"true\"}",
            )
            val model2 = MeasurementModelFactory.build(resultId = resultId2)
            val modelId1 = subject.createOrUpdate(model1)
            subject.createOrUpdate(model2)

            val output = subject.selectTestKeys(listOf(descriptor)).first()

            assertEquals(1, output.size)
            with(output.first()) {
                assertEquals(modelId1, id)
                assertEquals(resultId1, resultId)
                assertEquals("true", testKeys?.blocking)
            }
        }
}
