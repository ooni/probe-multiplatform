package org.ooni.probe.domain

import co.touchlab.kermit.Logger
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

        val result = (
            submitMeasurementWithUser(report)
        ).flatMapError { submitLegacy(report) }

        return when (result) {
            is Success -> {
                handleSubmitOutcome(result.value.verificationStatus, result.value.submitError)
                // TODO: persist result.value.verificationStatus on the measurement so it can be
                //  surfaced in the UI later (follow-up issue to be filed).
                val newMeasurement = measurement.copy(
                    isUploaded = true,
                    isUploadFailed = false,
                    uploadFailureMessage = null,
                    reportId = result.value.reportId,
                    uid = result.value.uid,
                )
                updateMeasurement(newMeasurement)
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
                    reportId = MeasurementModel.ReportId(it.updatedReportId),
                    uid = it.measurementUid?.ifBlank { null }?.let(MeasurementModel::Uid),
                )
            }.mapError { it.cause }

    class SubmitFailed(
        cause: Throwable?,
    ) : Exception(cause)

    data class ResponseData(
        val uid: MeasurementModel.Uid?,
        val reportId: MeasurementModel.ReportId? = null,
        val verificationStatus: VerificationStatus = VerificationStatus.Unknown,
        val submitError: SubmitError? = null,
    )
}
