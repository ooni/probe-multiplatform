package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.ooni.engine.Engine.MkException
import org.ooni.engine.OonimkallBridge.SubmitMeasurementResults
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.passport.models.SubmitError
import org.ooni.passport.models.VerificationStatus
import org.ooni.probe.data.disk.DeleteFiles
import org.ooni.probe.data.disk.ReadFile
import org.ooni.probe.data.models.MeasurementModel
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
        // Already known to be unrecoverable: skip re-read/re-parse/re-submit and don't re-report it.
        if (measurement.isUploadFailedPermanently) return measurement

        val reportFilePath = measurement.reportFilePath ?: return measurement

        val report = readFile(reportFilePath)
        if (report.isNullOrBlank()) {
            Logger.w("Missing or empty measurement report file")
            measurement.id?.let { deleteMeasurementById(it) }
            return null
        }

        reportStructuralError(report)?.let { parseError ->
            // The report can never be parsed, so it can never be submitted. Mark it (via the
            // existing upload_failure_msg column) so the upload sweep skips it instead of retrying
            // it forever, keep the row and file, and report it once for diagnosis.
            val errorType = categorizeParseError(parseError)
            Logger.w(
                "Measurement report unparseable; skipping upload (type=$errorType)",
                ReportUnparseable("type=$errorType, $parseError"),
            )
            Instrumentation.reportTransaction(
                operation = "SubmitReportUnparseable",
                data = mapOf(
                    "test" to measurement.test.name,
                    "length" to report.length,
                    "error" to parseError,
                    "corruption_source" to "disk",
                    "parse_error_type" to errorType,
                ),
            )
            val marked = measurement.copy(
                isUploadFailed = true,
                uploadFailureMessage = "${MeasurementModel.REPORT_UNPARSEABLE_PREFIX} $parseError",
            )
            updateMeasurement(marked)
            return marked
        }

        val result = (
            submitMeasurementWithUser(report)
        ).flatMapError { submitLegacy(report) }

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

    /**
     * Structural JSON validity of the report. Returns null when valid, or a bounded parser message
     * (no raw content — it may contain IPs/network names) when not. Structural-only on purpose: a
     * valid-but-fieldless report may still submit via the legacy path, so we must not abandon it.
     */
    private fun reportStructuralError(report: String): String? =
        try {
            if (json.parseToJsonElement(report) is JsonObject) null else "root is not a JSON object"
        } catch (e: Exception) {
            e.message?.take(MAX_PARSE_ERROR_LENGTH) ?: "unparseable"
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
        private const val MAX_PARSE_ERROR_LENGTH = 200

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
