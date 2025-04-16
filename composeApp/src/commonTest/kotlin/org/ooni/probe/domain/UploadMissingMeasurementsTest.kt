package org.ooni.probe.domain

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.ooni.engine.Engine
import org.ooni.engine.OonimkallBridge
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Success
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementsFilter
import org.ooni.testing.factories.MeasurementModelFactory
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UploadMissingMeasurementsTest {
    @Test
    fun uploadSuccessful() =
        runTest {
            val model = MeasurementModelFactory.build(
                id = MeasurementModel.Id(Random.nextLong().absoluteValue),
            )
            var newModel: MeasurementModel? = null
            val subject = UploadMissingMeasurements(
                getMeasurementsNotUploaded = { flowOf(listOf(model)) },
                submitMeasurement = {
                    Success(
                        OonimkallBridge.SubmitMeasurementResults(
                            updatedMeasurement = "",
                            updatedReportId = "report_id",
                        ),
                    )
                },
                readFile = { "{}" },
                deleteFiles = { },
                updateMeasurement = { newModel = it },
                deleteMeasurementById = { },
            )

            val results = mutableListOf<UploadMissingMeasurements.State>()
            subject(MeasurementsFilter.All).collect { results.add(it) }

            assertEquals(UploadMissingMeasurements.State.Starting, results[0])
            assertEquals(UploadMissingMeasurements.State.Uploading(0, 0, 1), results[1])
            assertEquals(UploadMissingMeasurements.State.Finished(1, 0, 1), results[2])
            assertTrue(newModel!!.isUploaded)
            assertFalse(newModel!!.isUploadFailed)
            assertEquals("report_id", newModel!!.reportId!!.value)
        }

    @Test
    fun uploadFailedWhenSubmitting() =
        runTest {
            val model = MeasurementModelFactory.build(
                id = MeasurementModel.Id(Random.nextLong().absoluteValue),
            )
            var newModel: MeasurementModel? = null
            val subject = UploadMissingMeasurements(
                getMeasurementsNotUploaded = { flowOf(listOf(model)) },
                submitMeasurement = { Failure(Engine.MkException(Exception("failed"))) },
                readFile = { "{}" },
                deleteFiles = { },
                updateMeasurement = { newModel = it },
                deleteMeasurementById = { },
            )

            val results = mutableListOf<UploadMissingMeasurements.State>()
            subject(MeasurementsFilter.All).collect { results.add(it) }

            assertEquals(UploadMissingMeasurements.State.Starting, results[0])
            assertEquals(UploadMissingMeasurements.State.Uploading(0, 0, 1), results[1])
            assertEquals(UploadMissingMeasurements.State.Finished(0, 1, 1), results[2])
            assertEquals("failed", newModel!!.uploadFailureMessage)
        }

    @Test
    fun uploadFailedWhenReadingEmptyFile() =
        runTest {
            val model = MeasurementModelFactory.build(
                id = MeasurementModel.Id(Random.nextLong().absoluteValue),
            )
            var updateMeasurementCalled = false
            var deleteMeasurementCalled = false
            val subject = UploadMissingMeasurements(
                getMeasurementsNotUploaded = { flowOf(listOf(model)) },
                submitMeasurement = { Failure(Engine.MkException(Exception("failed"))) },
                readFile = { "" },
                deleteFiles = { },
                updateMeasurement = { updateMeasurementCalled = true },
                deleteMeasurementById = { deleteMeasurementCalled = true },
            )

            val results = mutableListOf<UploadMissingMeasurements.State>()
            subject(MeasurementsFilter.All).collect { results.add(it) }

            assertEquals(UploadMissingMeasurements.State.Starting, results[0])
            assertEquals(UploadMissingMeasurements.State.Uploading(0, 0, 1), results[1])
            assertEquals(UploadMissingMeasurements.State.Finished(0, 1, 1), results[2])
            assertFalse(updateMeasurementCalled)
            assertTrue(deleteMeasurementCalled)
        }
}
