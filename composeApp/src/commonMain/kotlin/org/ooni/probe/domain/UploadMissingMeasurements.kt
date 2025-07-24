package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import org.ooni.engine.Engine.MkException
import org.ooni.engine.OonimkallBridge.SubmitMeasurementResults
import org.ooni.engine.models.Result
import org.ooni.probe.data.disk.DeleteFiles
import org.ooni.probe.data.disk.ReadFile
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementsFilter
import org.ooni.probe.shared.monitoring.Instrumentation

class UploadMissingMeasurements(
    private val getMeasurementsNotUploaded: (MeasurementsFilter) -> Flow<List<MeasurementModel>>,
    private val submitMeasurement: suspend (String) -> Result<SubmitMeasurementResults, MkException>,
    private val readFile: ReadFile,
    private val deleteFiles: DeleteFiles,
    private val updateMeasurement: suspend (MeasurementModel) -> Unit,
    private val deleteMeasurementById: suspend (MeasurementModel.Id) -> Unit,
) {
    operator fun invoke(filter: MeasurementsFilter): Flow<State> =
        channelFlow {
            Instrumentation.withTransaction(
                operation = this@UploadMissingMeasurements::class.simpleName.orEmpty(),
                data = mapOf(
                    "resultId" to
                        (filter as? MeasurementsFilter.Result)?.resultId?.value.toString(),
                    "measurementId" to
                        (filter as? MeasurementsFilter.Measurement)?.measurementId?.value.toString(),
                ),
            ) {
                send(State.Starting)

                val measurements = getMeasurementsNotUploaded(filter).first()
                val total = measurements.size
                var uploaded = 0
                var failedToUpload = 0
                var subsequentFailures = 0

                if (total > 0) {
                    Logger.i("Uploading missing measurements: $total")
                }

                measurements.forEach { measurement ->
                    if (!isActive) return@withTransaction // Check is coroutine was cancelled

                    if (subsequentFailures >= MAX_SUBSEQUENT_FAILURES) {
                        Logger.i("Aborting upload due to too many subsequent failures")
                        send(State.Finished(uploaded, failedToUpload, total))
                        return@withTransaction
                    }

                    send(State.Uploading(uploaded, failedToUpload, total))

                    val reportFilePath = measurement.reportFilePath ?: run {
                        failedToUpload++
                        subsequentFailures++
                        return@forEach
                    }
                    val report = readFile(reportFilePath)
                    if (report.isNullOrBlank()) {
                        Logger.w("Missing or empty measurement report file")
                        failedToUpload++
                        subsequentFailures++
                        measurement.id?.let { deleteMeasurementById(it) }
                        return@forEach
                    }

                    submitMeasurement(report)
                        .onSuccess { submitResult ->
                            uploaded++
                            subsequentFailures = 0
                            updateMeasurement(
                                measurement.copy(
                                    isUploaded = true,
                                    isUploadFailed = false,
                                    uploadFailureMessage = null,
                                    reportId =
                                        MeasurementModel.ReportId(submitResult.updatedReportId),
                                    uid = submitResult.measurementUid?.let(MeasurementModel::Uid),
                                ),
                            )
                            deleteFiles(reportFilePath)
                        }.onFailure { exception ->
                            failedToUpload++
                            subsequentFailures++
                            updateMeasurement(
                                measurement.copy(
                                    isUploadFailed = true,
                                    uploadFailureMessage = exception.cause?.message,
                                ),
                            )
                            Logger.w("Failed to submit measurement", SubmitFailed(exception))
                        }
                }

                send(State.Finished(uploaded, failedToUpload, total))
            }
        }

    sealed interface State {
        data object Starting : State

        data class Uploading(
            val uploaded: Int,
            val failedToUpload: Int,
            val total: Int,
        ) : State {
            val progressText
                get() = "${uploaded + failedToUpload + 1}/$total"
        }

        data class Finished(
            val uploaded: Int,
            val failedToUpload: Int,
            val total: Int,
        ) : State
    }

    class SubmitFailed(
        cause: Exception,
    ) : Exception(cause)

    companion object {
        private const val MAX_SUBSEQUENT_FAILURES = 5
    }
}
