package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import org.ooni.engine.Engine.MkException
import org.ooni.engine.OonimkallBridge.SubmitMeasurementResults
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.probe.data.disk.DeleteFiles
import org.ooni.probe.data.disk.ReadFile
import org.ooni.probe.data.models.MeasurementModel

class SubmitMeasurement(
    private val engineSubmit: suspend (String) -> Result<SubmitMeasurementResults, MkException>,
    private val readFile: ReadFile,
    private val deleteFiles: DeleteFiles,
    private val updateMeasurement: suspend (MeasurementModel) -> Unit,
    private val deleteMeasurementById: suspend (MeasurementModel.Id) -> Unit,
) {
    suspend operator fun invoke(measurement: MeasurementModel): MeasurementModel? {
        Logger.i("Event: submit")
        val reportFilePath = measurement.reportFilePath ?: return measurement
        Logger.i("Event: submit next")

        val report = readFile(reportFilePath)
        if (report.isNullOrBlank()) {
            Logger.w("Missing or empty measurement report file")
            measurement.id?.let { deleteMeasurementById(it) }
            return null
        }

        return when (val result = engineSubmit(report)) {
            is Success -> {
                val newMeasurement = measurement.copy(
                    isUploaded = true,
                    isUploadFailed = false,
                    uploadFailureMessage = null,
                    reportId = MeasurementModel.ReportId(result.value.updatedReportId),
                    uid = result.value.measurementUid?.let(MeasurementModel::Uid),
                )
                updateMeasurement(newMeasurement)
                deleteFiles(reportFilePath)
                newMeasurement
            }

            is Failure -> {
                val newMeasurement = measurement.copy(
                    isUploadFailed = true,
                    uploadFailureMessage = result.reason.cause?.message,
                )
                updateMeasurement(newMeasurement)
                Logger.w("Failed to submit measurement", SubmitFailed(result.reason))
                newMeasurement
            }
        }
    }

    class SubmitFailed(
        cause: Exception,
    ) : Exception(cause)
}
