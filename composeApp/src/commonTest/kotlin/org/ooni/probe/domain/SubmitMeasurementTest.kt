package org.ooni.probe.domain

import kotlinx.coroutines.test.runTest
import org.ooni.engine.models.Success
import org.ooni.passport.models.VerificationStatus
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.di.Dependencies
import org.ooni.testing.factories.MeasurementModelFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubmitMeasurementTest {
    private fun buildSubject(
        responseData: SubmitMeasurement.ResponseData =
            SubmitMeasurement.ResponseData(uid = MeasurementModel.Uid("uid")),
        report: String = "{}",
        onRead: () -> Unit = {},
        onSubmit: () -> Unit = {},
        onUpdate: (MeasurementModel) -> Unit = {},
        onDeleteFile: () -> Unit = {},
        onDeleteById: () -> Unit = {},
    ) = SubmitMeasurement(
        submitMeasurementWithUser = {
            onSubmit()
            Success(responseData)
        },
        engineSubmit = { error("legacy submit should not be used") },
        readFile = {
            onRead()
            report
        },
        deleteFiles = { onDeleteFile() },
        updateMeasurement = { onUpdate(it) },
        deleteMeasurementById = { onDeleteById() },
        handleSubmitOutcome = { _, _ -> },
        json = Dependencies.buildJson(),
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

    @Test
    fun corruptReportIsMarkedPermanentAndNotSubmitted() =
        // (a) early EOF, (b) mid-stream unterminated string, (c) trailing malformed number.
        listOf(
            "{\"probe_cc\":",
            "{\"headers\":{\"Content-Security-Policy\":\"default-src ",
            "{\"test_runtime\":6.",
        ).forEach { corruptReport ->
            runTest {
                var updated: MeasurementModel? = null
                var submitted = false
                var fileDeleted = false
                var rowDeleted = false
                val subject = buildSubject(
                    report = corruptReport,
                    onSubmit = { submitted = true },
                    onUpdate = { updated = it },
                    onDeleteFile = { fileDeleted = true },
                    onDeleteById = { rowDeleted = true },
                )

                val result =
                    subject.invokeInstrumented(MeasurementModelFactory.build(id = MeasurementModel.Id(1L)))

                val markerMessage = updated?.uploadFailureMessage
                assertTrue(updated?.isUploadFailed == true, "isUploadFailed for <$corruptReport>")
                assertTrue(
                    markerMessage?.startsWith(MeasurementModel.REPORT_UNPARSEABLE_PREFIX) == true,
                    "marker message for <$corruptReport>",
                )
                assertTrue(
                    updated?.isUploadFailedPermanently == true,
                    "derived permanent flag for <$corruptReport>",
                )
                assertFalse(updated?.isUploaded == true, "not uploaded for <$corruptReport>")
                assertFalse(submitted, "network submit skipped for <$corruptReport>")
                assertFalse(fileDeleted, "report file kept for <$corruptReport>")
                assertFalse(rowDeleted, "row kept for <$corruptReport>")
                assertTrue(result?.isUploadFailedPermanently == true, "returns marked for <$corruptReport>")
            }
        }

    @Test
    fun alreadyPermanentIsNoOp() =
        runTest {
            var read = false
            var submitted = false
            var updated = false
            val subject = buildSubject(
                onRead = { read = true },
                onSubmit = { submitted = true },
                onUpdate = { updated = true },
            )

            val alreadyMarked = MeasurementModelFactory.build(
                id = MeasurementModel.Id(1L),
                isUploadFailed = true,
                uploadFailureMessage = "${MeasurementModel.REPORT_UNPARSEABLE_PREFIX} Unexpected EOF",
            )
            val result = subject.invokeInstrumented(alreadyMarked)

            assertEquals(alreadyMarked, result)
            assertFalse(read, "file not re-read")
            assertFalse(submitted, "not re-submitted")
            assertFalse(updated, "not re-written")
        }

    @Test
    fun validReportStillSubmits() =
        runTest {
            var submitted = false
            var updated: MeasurementModel? = null
            val subject = buildSubject(
                report = "{\"probe_cc\":\"IT\",\"probe_asn\":\"AS1\"}",
                onSubmit = { submitted = true },
                onUpdate = { updated = it },
            )

            subject.invokeInstrumented(MeasurementModelFactory.build(id = MeasurementModel.Id(1L)))

            assertTrue(submitted, "valid report is submitted")
            assertTrue(updated?.isUploaded == true)
            assertFalse(updated?.isUploadFailedPermanently == true)
        }
}
