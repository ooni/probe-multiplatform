package org.ooni.probe.data.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.ResultFilter
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.di.Dependencies
import org.ooni.probe.shared.today
import org.ooni.testing.createTestDatabaseDriver
import org.ooni.testing.factories.MeasurementModelFactory
import org.ooni.testing.factories.ResultModelFactory
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ResultRepositoryTest {
    private lateinit var subject: ResultRepository
    private lateinit var measurementRepository: MeasurementRepository
    private val json = Dependencies.buildJson()

    @BeforeTest
    fun before() {
        val database = Dependencies.buildDatabase(::createTestDatabaseDriver)
        subject = ResultRepository(database, Dispatchers.Default)
        measurementRepository = MeasurementRepository(
            database = database,
            backgroundContext = Dispatchers.Default,
            json = json,
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
                origin: TaskOrigin?,
            ) {
                assertEquals(
                    expectedSize,
                    subject.list(ResultFilter(taskOrigin = origin)).first().size,
                )
            }

            assertResultSize(1, null)
            assertResultSize(1, TaskOrigin.OoniRun)
            assertResultSize(0, TaskOrigin.AutoRun)
        }

    @Test
    fun filterByDate() =
        runTest {
            val today = LocalDate.today()
            val yesterday = today.minus(DatePeriod(days = 1))
            val modelToday = ResultModelFactory.build(
                id = ResultModel.Id(Random.nextLong().absoluteValue),
                startTime = today.atTime(5, 30, 0),
            )
            val modelYesterday = ResultModelFactory.build(
                id = ResultModel.Id(Random.nextLong().absoluteValue),
                startTime = yesterday.atTime(5, 30, 0),
            )
            subject.createOrUpdate(modelToday)
            subject.createOrUpdate(modelYesterday)

            assertEquals(
                1,
                subject.list(
                    ResultFilter(dates = ResultFilter.Date.Custom(today..today)),
                ).first().size,
            )
            assertEquals(
                2,
                subject.list(
                    ResultFilter(dates = ResultFilter.Date.Custom(yesterday..today)),
                ).first().size,
            )
        }

    @Test
    fun countMissingUpload() =
        runTest {
            val result = ResultModelFactory.build()
            subject.createOrUpdate(result)
            assertEquals(0, subject.countMissingUpload().first())

            val measurement = MeasurementModelFactory.build(
                id = MeasurementModel.Id(1),
                resultId = result.id!!,
                isDone = true,
                isUploaded = false,
            )
            measurementRepository.createOrUpdate(measurement)
            assertEquals(1, subject.countMissingUpload().first())

            measurementRepository.createOrUpdate(
                measurement.copy(
                    isUploaded = true,
                    reportId = MeasurementModel.ReportId("1234"),
                ),
            )
            assertEquals(0, subject.countMissingUpload().first())
        }

    @Test
    fun markAsViewed() =
        runTest {
            val model = ResultModelFactory.build(isViewed = false)
            subject.createOrUpdate(model)

            subject.markAsViewed(model.id!!)

            assertTrue(subject.getById(model.id!!).first()!!.first.isViewed)
        }

    @Test
    fun markAllAsViewed() =
        runTest {
            subject.createOrUpdate(ResultModelFactory.build(isViewed = false))

            subject.markAllAsViewed(ResultFilter())

            assertTrue(subject.getLatest().first()!!.isViewed)
        }
}
