package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.MeasurementWithUrl
import org.ooni.probe.data.models.NetworkModel
import org.ooni.probe.data.models.ResultItem
import org.ooni.probe.data.models.ResultModel

class GetResult(
    private val getResultById: (ResultModel.Id) -> Flow<Pair<ResultModel, NetworkModel?>?>,
    private val getTestDescriptors: Flow<List<Descriptor>>,
    private val getMeasurementsByResultId: (ResultModel.Id) -> Flow<List<MeasurementWithUrl>>,
) {
    operator fun invoke(resultId: ResultModel.Id): Flow<ResultItem?> =
        combine(
            getResultById(resultId),
            getTestDescriptors,
            getMeasurementsByResultId(resultId),
        ) { resultWithNetwork, descriptors, measurements ->
            val result = resultWithNetwork?.first ?: return@combine null
            ResultItem(
                result = result,
                descriptor = descriptors.forResult(result) ?: return@combine null,
                network = resultWithNetwork.second,
                measurements = measurements,
            )
        }
}
