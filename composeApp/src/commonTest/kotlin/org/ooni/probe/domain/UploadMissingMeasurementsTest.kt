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
}
