package org.ooni.probe.domain

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.ooni.engine.Engine.MkException
import org.ooni.engine.models.Result
import org.ooni.probe.data.models.InstalledTestDescriptorModel

class FetchDescriptorUpdate(
    private val fetchDescriptor: suspend (descriptorId: String) -> Result<InstalledTestDescriptorModel?, MkException>,
    private val createOrUpdateTestDescriptors: suspend (List<InstalledTestDescriptorModel>) -> Unit,
) {
    suspend operator fun invoke(
        descriptors: List<InstalledTestDescriptorModel>,
    ): MutableMap<ResultStatus, MutableList<Result<InstalledTestDescriptorModel?, MkException>>> {
        val response = coroutineScope {
            descriptors.map { descriptor ->
                async {
                    Pair(descriptor, fetchDescriptor(descriptor.id.value.toString()))
                }
            }.awaitAll()
        }
        val autoUpdateItems = mutableListOf<InstalledTestDescriptorModel>()
        val resultsMap = mutableMapOf<ResultStatus, MutableList<Result<InstalledTestDescriptorModel?, MkException>>>()
        ResultStatus.entries.forEach { resultsMap[it] = mutableListOf() }
        response.forEach { (descriptor, result) ->
            val status: ResultStatus = if (descriptor.autoUpdate) {
                var res: ResultStatus = ResultStatus.NoUpdates
                result.map { updatedDescriptor ->
                    updatedDescriptor?.let {
                        if (descriptor.shouldUpdate(it)) {
                            autoUpdateItems.add(it.copy(autoUpdate = descriptor.autoUpdate))
                            res = ResultStatus.AutoUpdated
                        }
                    }
                }
                res
            } else {
                var res: ResultStatus = ResultStatus.NoUpdates
                result.map { updatedDescriptor ->
                    updatedDescriptor?.let {
                        if (descriptor.shouldUpdate(updatedDescriptor)) {
                            res = ResultStatus.UpdatesAvailable
                        }
                    }
                }
                res
            }
            resultsMap[status]?.add(result.map { it?.copy(autoUpdate = descriptor.autoUpdate) })
        }

        createOrUpdateTestDescriptors(autoUpdateItems)

        return resultsMap
    }
}

enum class ResultStatus {
    AutoUpdated,
    NoUpdates,
    UpdatesAvailable,
}
