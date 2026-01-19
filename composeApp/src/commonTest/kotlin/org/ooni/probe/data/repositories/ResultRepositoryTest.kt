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
                subject
                    .list(
                        ResultFilter(dates = ResultFilter.Date.Custom(today..today)),
                    ).first()
                    .size,
            )
            assertEquals(
                2,
                subject
                    .list(
                        ResultFilter(dates = ResultFilter.Date.Custom(yesterday..today)),
                    ).first()
                    .size,
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

            assertTrue(
                subject
                    .getById(model.id)
                    .first()!!
                    .first.isViewed,
            )
        }

    @Test
    fun markAllAsViewed() =
        runTest {
            subject.createOrUpdate(ResultModelFactory.build(isViewed = false))

            subject.markAllAsViewed(ResultFilter())

            assertTrue(subject.getLatest().first()!!.isViewed)
        }

    @Test
    fun countAllNotViewed() =
        runTest {
            // Create a viewed result (shouldn't be counted)
            val viewedResult = ResultModelFactory.build(
                id = ResultModel.Id(Random.nextLong().absoluteValue),
                isViewed = true,
                isDone = true,
            )
            subject.createOrUpdate(viewedResult)

            // Verify initial count is 0 since the only result is viewed
            assertEquals(0, subject.countAllNotViewedFlow().first(), "Initial count should be 0 with only viewed results")

            // Create an unviewed but not done result (shouldn't be counted)
            val notDoneResult = ResultModelFactory.build(
                id = ResultModel.Id(Random.nextLong().absoluteValue),
                isViewed = false,
                isDone = false,
            )
            subject.createOrUpdate(notDoneResult)

            // Verify still 0 since we added an unviewed but not done result
            assertEquals(0, subject.countAllNotViewedFlow().first(), "Count should still be 0 after adding undone result")

            // Create an unviewed done result with no measurements (should not be counted)
            val unviewedDoneResult = ResultModelFactory.build(
                id = ResultModel.Id(Random.nextLong().absoluteValue),
                isViewed = false,
                isDone = true,
            )
            subject.createOrUpdate(unviewedDoneResult)

            // Verify count is 0 after adding unviewed done result with no measurements
            assertEquals(
                0,
                subject.countAllNotViewedFlow().first(),
                "Count should be 1 after adding unviewed done result with no measurements",
            )

            // Create an unviewed done result with a done measurement (should be counted)
            val unviewedWithMeasurement = ResultModelFactory.build(
                id = ResultModel.Id(Random.nextLong().absoluteValue),
                isViewed = false,
                isDone = true,
            )
            val resultId = subject.createOrUpdate(unviewedWithMeasurement)
            val measurement = MeasurementModelFactory.build(
                resultId = resultId,
                isDone = true,
            )
            measurementRepository.createOrUpdate(measurement)

            // Verify count is 1 after adding unviewed done result with done measurement
            assertEquals(
                1,
                subject.countAllNotViewedFlow().first(),
                "Count should be 1 after adding unviewed done result with done measurement",
            )

            // Create an unviewed done result with an undone measurement (should be counted)
            val unviewedWithUndoneMeasurement = ResultModelFactory.build(
                id = ResultModel.Id(Random.nextLong().absoluteValue),
                isViewed = false,
                isDone = true,
            )
            val resultId2 = subject.createOrUpdate(unviewedWithUndoneMeasurement)
            measurementRepository.createOrUpdate(
                MeasurementModelFactory.build(
                    resultId = resultId2,
                    isDone = false,
                ),
            )
            measurementRepository.createOrUpdate(
                MeasurementModelFactory.build(
                    resultId = resultId2,
                    isDone = true,
                ),
            )

            // Still expect 1 since the last result has only undone measurements
            assertEquals(
                2,
                subject.countAllNotViewedFlow().first(),
                "Count should still be 2 after adding result with only undone measurements",
            )

            // After marking one as viewed, count should decrease
            subject.markAsViewed(unviewedWithUndoneMeasurement.id!!)
            assertEquals(1, subject.countAllNotViewedFlow().first(), "Count should decrease to 1 after marking a result as viewed")
        }
}
