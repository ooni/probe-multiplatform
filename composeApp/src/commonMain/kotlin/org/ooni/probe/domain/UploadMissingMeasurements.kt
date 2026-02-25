package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementsFilter
import org.ooni.probe.shared.monitoring.Instrumentation

class UploadMissingMeasurements(
    private val getMeasurementsNotUploaded: (MeasurementsFilter) -> Flow<List<MeasurementModel>>,
    private val submitMeasurement: suspend (MeasurementModel) -> MeasurementModel?,
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

                    val newMeasurement = submitMeasurement(measurement)

                    if (newMeasurement?.isUploaded == true) {
                        uploaded++
                        subsequentFailures = 0
                    } else {
                        failedToUpload++
                        subsequentFailures++
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

    companion object {
        private const val MAX_SUBSEQUENT_FAILURES = 5
    }
}
