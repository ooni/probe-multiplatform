package org.ooni.probe.data.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.ResultFilter
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.di.Dependencies
import org.ooni.testing.createTestDatabaseDriver
import org.ooni.testing.factories.ResultModelFactory
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ResultRepositoryTest {
    private lateinit var subject: ResultRepository

    @BeforeTest
    fun before() {
        subject =
            ResultRepository(
                database = Dependencies.buildDatabase(::createTestDatabaseDriver),
                backgroundContext = Dispatchers.Default,
            )
    }

    @Test
    fun createWithIdAndGet() =
        runTest {
            val model =
                ResultModelFactory.build(id = ResultModel.Id(Random.nextLong().absoluteValue))

            val modelId = subject.createOrUpdate(model)
            val result = subject.list().first().first()

            assertEquals(model, result.result)
            assertEquals(model.id, modelId)
        }

    @Test
    fun createWithoutId() =
        runTest {
            val model = ResultModelFactory.build(id = null)

            val modelId = subject.createOrUpdate(model)

            assertNotNull(modelId)
        }

    @Test
    fun filterByTaskOrigin() =
        runTest {
            val model = ResultModelFactory.build(
                id = ResultModel.Id(Random.nextLong().absoluteValue),
                taskOrigin = TaskOrigin.OoniRun,
            )
            subject.createOrUpdate(model)

            suspend fun assertResultSize(
                expectedSize: Int,
                filter: ResultFilter.Type<TaskOrigin>,
            ) {
                assertEquals(
                    expectedSize,
                    subject.list(ResultFilter(taskOrigin = filter)).first().size,
                )
            }

            assertResultSize(1, ResultFilter.Type.All)
            assertResultSize(1, ResultFilter.Type.One(TaskOrigin.OoniRun))
            assertResultSize(0, ResultFilter.Type.One(TaskOrigin.AutoRun))
        }
}
