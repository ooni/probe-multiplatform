package org.ooni.probe.domain

import kotlinx.coroutines.test.runTest
import org.ooni.engine.OonimkallBridge
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Success
import org.ooni.passport.models.PassportException
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
                    verificationStatus = VerificationStatus.Unknown,
                ),
                onUpdate = { updated = it },
            )

            subject.invokeInstrumented(MeasurementModelFactory.build(id = MeasurementModel.Id(1L)))

            assertNull(updated?.verificationStatus)
        }

    /**
     * The legacy engine upload is a separate HTTP stack that the Passport offline gate does not
     * cover, so it has to be skipped explicitly - otherwise "fail fast offline" still ends up
     * blocking on a socket that cannot connect.
     */
    @Test
    fun offlineSubmitSkipsBothTheAuthenticatedAndLegacyUpload() =
        runTest {
            var legacySubmits = 0
            var updated: MeasurementModel? = null
            val subject = SubmitMeasurement(
                submitMeasurementWithUser = {
                    Failure(PassportException.Offline("no active network"))
                },
                engineSubmit = {
                    legacySubmits++
                    error("legacy submit must not run while offline")
                },
                readFile = { "{}" },
                deleteFiles = { },
                updateMeasurement = { updated = it },
                deleteMeasurementById = { },
                handleSubmitOutcome = { _, _ -> },
            )

            subject.invokeInstrumented(MeasurementModelFactory.build(id = MeasurementModel.Id(1L)))

            assertEquals(0, legacySubmits)
            // Still uploadable: UploadMissingMeasurements selects on !isUploaded, so the
            // measurement is retried once there is a network again.
            assertTrue(updated?.isUploadFailed == true)
            assertTrue(updated?.isUploaded == false)
            assertTrue(updated?.isMissingUpload == true)
        }

    @Test
    fun nonOfflineFailureStillFallsBackToTheLegacyUpload() =
        runTest {
            var legacySubmits = 0
            val subject = SubmitMeasurement(
                submitMeasurementWithUser = {
                    Failure(PassportException.HttpClientError("Submit returned HTTP 500"))
                },
                engineSubmit = {
                    legacySubmits++
                    Success(
                        OonimkallBridge.SubmitMeasurementResults(
                            updatedMeasurement = "{}",
                            updatedReportId = "report-id",
                            measurementUid = "uid",
                        ),
                    )
                },
                readFile = { "{}" },
                deleteFiles = { },
                updateMeasurement = { },
                deleteMeasurementById = { },
                handleSubmitOutcome = { _, _ -> },
            )

            subject.invokeInstrumented(MeasurementModelFactory.build(id = MeasurementModel.Id(1L)))

            assertEquals(1, legacySubmits, "a server error must still try the legacy upload")
        }
}
