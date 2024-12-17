package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.ResultFilter
import org.ooni.probe.data.models.ResultListItem
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.ResultWithNetworkAndAggregates

class GetResults(
    private val getResults: (ResultFilter) -> Flow<List<ResultWithNetworkAndAggregates>>,
    private val getDescriptors: () -> Flow<List<Descriptor>>,
) {
    operator fun invoke(filter: ResultFilter): Flow<List<ResultListItem>> =
        combine(
            getResults(filter),
            getDescriptors(),
        ) { results, descriptors ->
            results.mapNotNull { item ->
                ResultListItem(
                    result = item.result,
                    descriptor = descriptors.forResult(item.result) ?: return@mapNotNull null,
                    network = item.network,
                    measurementCounts = item.measurementCounts,
                    allMeasurementsUploaded = item.allMeasurementsUploaded,
                    anyMeasurementUploadFailed = item.anyMeasurementUploadFailed,
                )
            }
        }
}

fun List<Descriptor>.forResult(result: ResultModel): Descriptor? =
    result.testDescriptorId
        ?.let { descriptorId ->
            firstOrNull {
                it.source is Descriptor.Source.Installed && it.source.value.id == descriptorId
            }
        }
        ?: firstOrNull { it.name == result.testGroupName }
