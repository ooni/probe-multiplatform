package org.ooni.probe.domain

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementsFilter
import org.ooni.testing.factories.MeasurementModelFactory
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UploadMissingMeasurementsTest {
    @Test
    fun uploadSuccessful() =
        runTest {
            val model = MeasurementModelFactory.build(
                id = MeasurementModel.Id(Random.nextLong().absoluteValue),
            )
            val subject = UploadMissingMeasurements(
                getMeasurementsNotUploaded = { flowOf(listOf(model)) },
                submitMeasurement = { model.copy(isUploaded = true) },
            )

            val results = mutableListOf<UploadMissingMeasurements.State>()
            subject(MeasurementsFilter.All).collect { results.add(it) }

            assertEquals(UploadMissingMeasurements.State.Starting, results[0])
            assertEquals(UploadMissingMeasurements.State.Uploading(0, 0, 1), results[1])
            assertEquals(UploadMissingMeasurements.State.Finished(1, 0, 1), results[2])
        }

    @Test
    fun uploadFailedWhenSubmitting() =
        runTest {
            val model = MeasurementModelFactory.build(
                id = MeasurementModel.Id(Random.nextLong().absoluteValue),
            )
            val subject = UploadMissingMeasurements(
                getMeasurementsNotUploaded = { flowOf(listOf(model)) },
                submitMeasurement = { model.copy(isUploaded = false) },
            )

            val results = mutableListOf<UploadMissingMeasurements.State>()
            subject(MeasurementsFilter.All).collect { results.add(it) }

            assertEquals(UploadMissingMeasurements.State.Starting, results[0])
            assertEquals(UploadMissingMeasurements.State.Uploading(0, 0, 1), results[1])
            assertEquals(UploadMissingMeasurements.State.Finished(0, 1, 1), results[2])
        }

    @Test
    fun permanentlyFailedRowsDoNotAbortTheSweep() =
        runTest {
            // 5 unparseable reports (the abort threshold) at the front, one healthy row behind them.
            val corrupt = (1..5).map {
                MeasurementModelFactory.build(
                    id = MeasurementModel.Id(it.toLong()),
                    isUploadFailed = true,
                    uploadFailureMessage =
                        "${MeasurementModel.REPORT_UNPARSEABLE_PREFIX} Unexpected EOF",
                )
            }
            val healthy = MeasurementModelFactory.build(id = MeasurementModel.Id(6L))

            var healthyUploaded = false
            val subject = UploadMissingMeasurements(
                getMeasurementsNotUploaded = { flowOf(corrupt + healthy) },
                submitMeasurement = { measurement ->
                    if (measurement.isUploadFailedPermanently) {
                        measurement // skipped by the sweep, stays permanently failed
                    } else {
                        healthyUploaded = true
                        measurement.copy(isUploaded = true)
                    }
                },
            )

            val results = mutableListOf<UploadMissingMeasurements.State>()
            subject(MeasurementsFilter.All).collect { results.add(it) }

            assertTrue(healthyUploaded, "healthy measurement behind the corrupt ones is still uploaded")
            assertEquals(UploadMissingMeasurements.State.Finished(1, 5, 6), results.last())
        }
}
