package org.ooni.probe.domain

import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.Success
import org.ooni.passport.models.VerificationStatus
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.testing.factories.MeasurementModelFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubmitMeasurementTest {
    private fun buildSubject(
        responseData: SubmitMeasurement.ResponseData,
        onUpdate: (MeasurementModel) -> Unit,
    ) = SubmitMeasurement(
        submitMeasurementWithUser = { Success(responseData) },
        engineSubmit = { error("legacy submit should not be used") },
        readFile = { "{}" },
        deleteFiles = { },
        updateMeasurement = { onUpdate(it) },
        deleteMeasurementById = { },
        handleSubmitOutcome = { _, _ -> },
    )

    @Test
    fun persistsVerificationStatusOnSuccess() =
        runTest {
            var updated: MeasurementModel? = null
            val subject = buildSubject(
                responseData = SubmitMeasurement.ResponseData(
                    uid = MeasurementModel.Uid("uid"),
                    reportId = MeasurementModel.ReportId("rid"),
                    verificationStatus = VerificationStatus.Verified,
                ),
                onUpdate = { updated = it },
            )

            subject.invokeInstrumented(MeasurementModelFactory.build(id = MeasurementModel.Id(1L)))

            assertEquals(VerificationStatus.Verified, updated?.verificationStatus)
            assertTrue(updated?.isUploaded == true)
        }

    @Test
    fun unknownVerificationStatusStoredAsNull() =
        runTest {
            var updated: MeasurementModel? = null
            val subject = buildSubject(
                responseData = SubmitMeasurement.ResponseData(
                    uid = MeasurementModel.Uid("uid"),
                    reportId = MeasurementModel.ReportId("rid"),
                    verificationStatus = VerificationStatus.Unknown,
                ),
                onUpdate = { updated = it },
            )

            subject.invokeInstrumented(MeasurementModelFactory.build(id = MeasurementModel.Id(1L)))

            assertNull(updated?.verificationStatus)
        }
}
