package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementsFilter
import org.ooni.probe.data.models.ResultModel

class GetMeasurementsNotUploaded(
    private val listMeasurementsNotUploaded: (ResultModel.Id?) -> Flow<List<MeasurementModel>>,
    private val getMeasurementById: (MeasurementModel.Id) -> Flow<MeasurementModel?>,
) {
    fun invoke(filter: MeasurementsFilter): Flow<List<MeasurementModel>> =
        when (filter) {
            MeasurementsFilter.All -> listMeasurementsNotUploaded(null)
            is MeasurementsFilter.Result -> listMeasurementsNotUploaded(filter.resultId)
            is MeasurementsFilter.Measurement -> getMeasurementById(filter.measurementId).map {
                if (it == null || it.isDoneAndMissingUpload) emptyList() else listOf(it)
            }
        }
}
