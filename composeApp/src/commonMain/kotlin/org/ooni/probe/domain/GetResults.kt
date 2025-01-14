package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.ResultFilter
import org.ooni.probe.data.models.ResultListItem
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.ResultWithNetworkAndAggregates
import org.ooni.probe.data.models.TestKeysWithResultId

class GetResults(
    private val getResults: (ResultFilter) -> Flow<List<ResultWithNetworkAndAggregates>>,
    private val getDescriptors: () -> Flow<List<Descriptor>>,
    private val getTestKeys: (String?) -> Flow<List<TestKeysWithResultId>>,
) {
    operator fun invoke(filter: ResultFilter): Flow<List<ResultListItem>> =
        combine(
            getResults(filter),
            getDescriptors(),
            getTestKeys((filter.descriptor as? ResultFilter.Type.One)?.value?.key),
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

fun List<Descriptor>.forResult(result: ResultModel): Descriptor? =
    result.descriptorKey
        ?.let { key ->
            firstOrNull {
                it.source is Descriptor.Source.Installed && it.source.value.key == key
            }
        }
        ?: firstOrNull { it.name == result.descriptorName }

fun List<TestKeysWithResultId>.forResult(result: ResultModel): List<TestKeysWithResultId>? =
    result.id
        ?.let { resultId ->
            filter {
                it.resultId.value == resultId.value
            }
        }
