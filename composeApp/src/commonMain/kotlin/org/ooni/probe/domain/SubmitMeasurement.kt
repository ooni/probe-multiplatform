package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.ooni.engine.Engine.MkException
import org.ooni.engine.OonimkallBridge.SubmitMeasurementResults
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.passport.models.SubmitError
import org.ooni.passport.models.VerificationStatus
import org.ooni.passport.models.isOfflineFailure
import org.ooni.probe.data.disk.DeleteFiles
import org.ooni.probe.data.disk.ReadFile
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.isAsnZero
import org.ooni.probe.shared.monitoring.Instrumentation
import org.ooni.probe.shared.monitoring.reportTransaction

class SubmitMeasurement(
    private val submitMeasurementWithUser: suspend (
        String,
    ) -> Result<ResponseData, Throwable?>,
    private val engineSubmit: suspend (String) -> Result<SubmitMeasurementResults, MkException>,
    private val readFile: ReadFile,
    private val deleteFiles: DeleteFiles,
    private val updateMeasurement: suspend (MeasurementModel) -> Unit,
    private val deleteMeasurementById: suspend (MeasurementModel.Id) -> Unit,
    private val handleSubmitOutcome: suspend (VerificationStatus, SubmitError?) -> Unit,
    private val json: Json,
) {
    suspend operator fun invoke(measurement: MeasurementModel): MeasurementModel? =
        Instrumentation.withTransaction(
            operation = "SubmitMeasurement",
            data = mapOf(
                "measurementTest" to measurement.test.name,
                "isFailed" to measurement.isFailed,
                "isUploadFailed" to measurement.isUploadFailed,
                "runtime" to measurement.runtime.toString(),
            ),
        ) {
            invokeInstrumented(measurement)
        }

    suspend fun invokeInstrumented(measurement: MeasurementModel): MeasurementModel? {
        val reportFilePath = measurement.reportFilePath ?: return measurement

        val report = readFile(reportFilePath)
        if (report.isNullOrBlank()) {
            Logger.w("Missing or empty measurement report file")
            measurement.id?.let { deleteMeasurementById(it) }
            return null
        }

        val reportObject = when (val parsed = parseReport(report)) {
            is ParsedReport.Valid -> parsed.value
            is ParsedReport.Invalid -> {
                // The report can never be parsed, so it can never be submitted. Mark it not-done so the
                // upload sweep (which requires is_done = 1) skips it instead of retrying it forever, and
                // so the UI shows it as failed; keep the row and file, and report it once for diagnosis.
                val errorType = categorizeParseError(parsed.error)
                Logger.w(
                    "Measurement report unparseable; skipping upload (type=$errorType)",
                    ReportUnparseable("type=$errorType"),
                )
                Instrumentation.reportTransaction(
                    operation = "SubmitReportUnparseable",
                    data = mapOf(
                        "test" to measurement.test.name,
                        "length" to report.length,
                        "corruption_source" to "disk",
                        "parse_error_type" to errorType,
                    ),
                )
                val marked = measurement.copy(
                    isDone = false,
                    isFailed = true,
                    failureMessage = "Report unparseable: $errorType",
                )
                updateMeasurement(marked)
                return marked
            }
        }

        if (reportProbeAsn(reportObject).isAsnZero()) {
            return Instrumentation.withTransaction(
                operation = "SubmitReportAsnZero",
                data = mapOf("test" to measurement.test.name),
            ) {
                Logger.w("Measurement ASN is 0; skipping upload")
                val marked = measurement.copy(
                    isDone = false,
                    isFailed = true,
                    failureMessage = "ASN is 0",
                )
                updateMeasurement(marked)
                marked
            }
        }

        val result = submitMeasurementWithUser(report)
            .flatMapError { reason ->
                // The legacy engine upload is a separate HTTP stack, so the Passport gate does not
                // cover it. Falling back while offline would just block on a socket that cannot
                // connect.
                if (reason.isOfflineFailure()) Failure(reason) else submitLegacy(report)
            }

        return when (result) {
            is Success -> {
                handleSubmitOutcome(result.value.verificationStatus, result.value.submitError)
                val newMeasurement = measurement.copy(
                    isUploaded = true,
                    isUploadFailed = false,
                    uploadFailureMessage = null,
                    uid = result.value.uid,
                    verificationStatus = result.value.verificationStatus
                        .takeIf { it != VerificationStatus.Unknown },
                )
                updateMeasurement(newMeasurement)
                Logger.i { "Measurement Submission successful: ${newMeasurement.uid}" }
                deleteFiles(reportFilePath)
                newMeasurement
            }

            is Failure -> {
                val newMeasurement = measurement.copy(
                    isUploadFailed = true,
                    uploadFailureMessage = result.reason?.message,
                )
                updateMeasurement(newMeasurement)
                Logger.w("Failed to submit measurement", SubmitFailed(result.reason))
                newMeasurement
            }
        }
    }

    private suspend fun submitLegacy(measurementData: String): Result<ResponseData, Throwable?> =
        engineSubmit(measurementData)
            .map {
                ResponseData(
                    uid = it.measurementUid?.ifBlank { null }?.let(MeasurementModel::Uid),
                )
            }.mapError { it.cause }

    private fun parseReport(report: String): ParsedReport =
        try {
            when (val element = json.parseToJsonElement(report)) {
                is JsonObject -> ParsedReport.Valid(element)
                else -> ParsedReport.Invalid("root is not a JSON object")
            }
        } catch (e: Exception) {
            ParsedReport.Invalid(e.message ?: "unparseable")
        }

    private fun reportProbeAsn(report: JsonObject): String? =
        try {
            report["probe_asn"]
                ?.jsonPrimitive
                ?.contentOrNull
        } catch (_: Exception) {
            null
        }

    private sealed interface ParsedReport {
        data class Valid(
            val value: JsonObject,
        ) : ParsedReport

        data class Invalid(
            val error: String,
        ) : ParsedReport
    }

    class SubmitFailed(
        cause: Throwable?,
    ) : Exception(cause)

    class ReportUnparseable(
        message: String?,
    ) : Exception(message)

    data class ResponseData(
        val uid: MeasurementModel.Uid?,
        val verificationStatus: VerificationStatus = VerificationStatus.Unknown,
        val submitError: SubmitError? = null,
    )

    companion object {
        /**
         * Categorizes JSON parse errors into coarse buckets for Sentry grouping and operational
         * triage. The categories mirror the three corrupt-measurement-report symptoms:
         * early_eof, mid_stream, and late_truncation.
         */
        private fun categorizeParseError(error: String): String =
            when {
                error.contains("EOF", ignoreCase = true) -> "early_eof"
                error.contains("Expected quotation mark", ignoreCase = true) -> "mid_stream"
                error.contains("Expected end of the object or comma", ignoreCase = true) -> "late_truncation"
                else -> "unknown"
            }
    }
}
