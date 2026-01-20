package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.ooni.probe.data.models.DescriptorItem
import org.ooni.probe.data.models.ResultFilter
import org.ooni.probe.data.models.ResultListItem
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.ResultWithNetworkAndAggregates
import org.ooni.probe.data.models.TestKeysWithResultId

class GetResults(
    private val getResults: (ResultFilter) -> Flow<List<ResultWithNetworkAndAggregates>>,
    private val getDescriptors: () -> Flow<List<DescriptorItem>>,
    private val getTestKeys: (List<DescriptorItem>) -> Flow<List<TestKeysWithResultId>>,
) {
    operator fun invoke(filter: ResultFilter): Flow<List<ResultListItem>> =
        combine(
            getResults(filter),
            getDescriptors(),
            getTestKeys(filter.descriptors),
        ) { results, descriptors, testKeys ->
            results.mapNotNull { item ->
                ResultListItem(
                    result = item.result,
                    descriptor = descriptors.forResult(item.result.id) ?: return@mapNotNull null,
                    network = item.network,
                    measurementCounts = item.measurementCounts,
                    allMeasurementsUploaded = item.allMeasurementsUploaded,
                    anyMeasurementUploadFailed = item.anyMeasurementUploadFailed,
                    testKeys = testKeys.forResult(item.result),
                )
            }
        }
}

fun List<DescriptorItem>.forResult(resultId: ResultModel.Id?): DescriptorItem? =
    resultId
        ?.let { key ->
            firstOrNull {
                it.descriptor.key.id == key
            }
        }
        ?: firstOrNull { it.descriptor.name == resultId?.value.toString() }

fun List<TestKeysWithResultId>.forResult(result: ResultModel): List<TestKeysWithResultId>? =
    result.id
        ?.let { resultId ->
            filter {
                it.resultId.value == resultId.value
            }
        }
