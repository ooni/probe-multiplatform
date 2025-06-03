package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.ooni.probe.data.models.MeasurementCounts
import org.ooni.probe.data.models.ResultItem
import org.ooni.probe.data.models.ResultListItem
import org.ooni.probe.data.models.ResultModel

class GetLastResultOfDescriptor(
    private val getLastResultDoneByDescriptor: (String) -> Flow<ResultModel.Id?>,
    private val getResultById: (ResultModel.Id) -> Flow<ResultItem?>,
) {
    operator fun invoke(descriptorKey: String): Flow<ResultListItem?> =
        getLastResultDoneByDescriptor(descriptorKey)
            .flatMapLatest { lastResultId ->
                lastResultId
                    ?.let(getResultById::invoke)
                    ?: flowOf(null)
            }
            .map { it?.toListItem() }

    private fun ResultItem.toListItem() =
        ResultListItem(
            result = result,
            descriptor = descriptor,
            network = network,
            measurementCounts = MeasurementCounts(
                done = measurements.count { it.measurement.isDone }.toLong(),
                failed = measurements.count { it.measurement.isFailed }.toLong(),
                anomaly = measurements.count { it.measurement.isAnomaly }.toLong(),
            ),
            allMeasurementsUploaded = measurements.none { it.measurement.isDoneAndMissingUpload },
            anyMeasurementUploadFailed = measurements.any {
                it.measurement.isDoneAndMissingUpload && it.measurement.isUploadFailed
            },
            testKeys = testKeys,
        )
}
