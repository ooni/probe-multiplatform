package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import org.ooni.engine.Engine.MkException
import org.ooni.engine.OonimkallBridge.SubmitMeasurementResults
import org.ooni.engine.models.Result
import org.ooni.probe.data.disk.DeleteFiles
import org.ooni.probe.data.disk.ReadFile
import org.ooni.probe.data.models.MeasurementModel

class UploadMissingMeasurements(
    private val getMeasurementsNotUploaded: Flow<List<MeasurementModel>>,
    private val submitMeasurement: suspend (String) -> Result<SubmitMeasurementResults, MkException>,
    private val readFile: ReadFile,
    private val deleteFiles: DeleteFiles,
    private val updateMeasurement: suspend (MeasurementModel) -> Unit,
) {
    operator fun invoke(): Flow<State> =
        channelFlow {
            send(State.Starting)

            val measurements = getMeasurementsNotUploaded.first()
            val total = measurements.size
            var uploaded = 0
            var failedToUpload = 0

            measurements.forEach { measurement ->
                send(State.Uploading(uploaded, failedToUpload, total))

                val reportFilePath = measurement.reportFilePath ?: run {
                    failedToUpload++
                    return@forEach
                }
                val report = readFile(reportFilePath) ?: run {
                    failedToUpload++
                    return@forEach
                }

                submitMeasurement(report)
                    .onSuccess { submitResult ->
                        uploaded++
                        updateMeasurement(
                            measurement.copy(
                                isUploaded = true,
                                isUploadFailed = false,
                                uploadFailureMessage = null,
                                reportId = MeasurementModel.ReportId(submitResult.updatedReportId),
                            ),
                        )
                        deleteFiles(reportFilePath)
                    }
                    .onFailure { exception ->
                        failedToUpload++
                        updateMeasurement(
                            measurement.copy(
                                isUploadFailed = true,
                                uploadFailureMessage = exception.cause?.message,
                            ),
                        )
                    }
            }

            send(State.Finished(uploaded, failedToUpload, total))
        }

    sealed interface State {
        data object Starting : State

        data class Uploading(
            val uploaded: Int,
            val failedToUpload: Int,
            val total: Int,
        ) : State

        data class Finished(
            val uploaded: Int,
            val failedToUpload: Int,
            val total: Int,
        ) : State
    }
}
