package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.ooni.probe.data.models.DescriptorItem
import org.ooni.probe.data.models.OoniTest
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
                    descriptor = descriptors.forResult(item.result) ?: return@mapNotNull null,
                    network = item.network,
                    measurementCounts = item.measurementCounts,
                    allMeasurementsUploaded = item.allMeasurementsUploaded,
                    anyMeasurementUploadFailed = item.anyMeasurementUploadFailed,
                    testKeys = testKeys.forResult(item.result),
                )
            }
        }
}

private fun matchesDescriptor(
    item: DescriptorItem,
    result: ResultModel,
): Boolean {
    val key = result.descriptorKey
    // A non-default descriptor matches if its key ID is the same as the result's descriptor key.
    val isNonDefaultMatch = !item.isDefault() && item.descriptor.key.id == key
    // A default descriptor matches if its test name corresponds to the result's descriptor name.
    val isDefaultMatch = item.isDefault() && OoniTest.fromId(item.descriptor.key.id.value)?.key == result.descriptorName

    return isNonDefaultMatch || isDefaultMatch
}

fun List<DescriptorItem>.forResult(result: ResultModel?): DescriptorItem? =
    result
        ?.let { result ->
            firstOrNull {
                matchesDescriptor(it, result)
            }
        }
        ?: firstOrNull { it.descriptor.name == result?.descriptorName }

fun List<TestKeysWithResultId>.forResult(result: ResultModel): List<TestKeysWithResultId>? =
    result.id
        ?.let { resultId ->
            filter {
                it.resultId.value == resultId.value
            }
        }
