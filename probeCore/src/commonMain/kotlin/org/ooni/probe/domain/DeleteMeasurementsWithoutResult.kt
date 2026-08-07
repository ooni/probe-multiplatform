package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okio.Path
import org.ooni.probe.data.models.MeasurementModel

class DeleteMeasurementsWithoutResult(
    private val getMeasurementsWithoutResult: suspend () -> Flow<List<MeasurementModel>>,
    private val deleteMeasurementsById: suspend (List<MeasurementModel.Id>) -> Unit,
    private val deleteFile: suspend (Path) -> Unit,
) {
    suspend fun invoke() {
        val measurementsToDelete = getMeasurementsWithoutResult().first()
        measurementsToDelete.forEach { measurement ->
            deleteFile(measurement.logFilePath)
            measurement.reportFilePath?.let { reportFilePath ->
                deleteFile(reportFilePath)
            }
        }
        deleteMeasurementsById(measurementsToDelete.mapNotNull { it.id })
    }
}
