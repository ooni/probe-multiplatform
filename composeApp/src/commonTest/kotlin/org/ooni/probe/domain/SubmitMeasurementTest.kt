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
                    subject.invokeInstrumented(
                        MeasurementModelFactory.build(id = MeasurementModel.Id(1L), isDone = true),
                    )

                // Marked not-done so the upload sweep (is_done = 1) skips it and the UI shows failed.
                assertFalse(updated?.isDone == true, "marked not-done for <$corruptReport>")
                assertTrue(updated?.isFailed == true, "marked failed for <$corruptReport>")
                assertTrue(
                    updated?.failureMessage?.startsWith("Report unparseable:") == true,
                    "failure message for <$corruptReport>",
                )
                assertFalse(updated?.isUploaded == true, "not uploaded for <$corruptReport>")
                assertFalse(submitted, "network submit skipped for <$corruptReport>")
                assertFalse(fileDeleted, "report file kept for <$corruptReport>")
                assertFalse(rowDeleted, "row kept for <$corruptReport>")
                assertFalse(result?.isDone == true, "returns marked not-done for <$corruptReport>")
            }
        }

    @Test
    fun corruptReportDoesNotPersistItsContentsInFailureMessage() =
        runTest {
            var updated: MeasurementModel? = null
            val report = "{\"sensitive_url\":\"https://private.example/path"
            val subject = buildSubject(
                report = report,
                onUpdate = { updated = it },
            )

            subject.invokeInstrumented(
                MeasurementModelFactory.build(id = MeasurementModel.Id(1L), isDone = true),
            )

            assertFalse(updated?.failureMessage?.contains("private.example") == true)
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

            subject.invokeInstrumented(
                MeasurementModelFactory.build(id = MeasurementModel.Id(1L), isDone = true),
            )

            assertTrue(submitted, "valid report is submitted")
            assertTrue(updated?.isUploaded == true)
            assertTrue(updated?.isDone == true)
        }
}
