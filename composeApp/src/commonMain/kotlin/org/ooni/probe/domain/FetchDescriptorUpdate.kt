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
    suspend operator fun invoke(descriptors: List<InstalledTestDescriptorModel>): List<ResultStatus> {
        val response = coroutineScope {
            descriptors.map { descriptor ->
                async {
                    Pair(descriptor, fetchDescriptor(descriptor.id.value.toString()))
                }
            }.awaitAll()
        }
        val autoUpdateItems = mutableListOf<InstalledTestDescriptorModel>()
        val result = response.map { pair ->
            if (pair.first.autoUpdate) {
                var res: ResultStatus = ResultStatus.NoUpdates(value = pair.second)
                pair.second.map { descriptor ->
                    descriptor?.let {
                        if (pair.first.shouldUpdate(descriptor)) {
                            autoUpdateItems.add(descriptor.copy(autoUpdate = pair.first.autoUpdate))
                            res = ResultStatus.AutoUpdated(value = pair.second)
                        }
                    }
                }
                return@map res
            } else {
                var res: ResultStatus = ResultStatus.NoUpdates(value = pair.second)
                pair.second.map { descriptor ->
                    descriptor?.let {
                        if (pair.first.shouldUpdate(descriptor)) {
                            res = ResultStatus.UpdatesAvailable(value = pair.second)
                        }
                    }
                }
                return@map res
            }
        }

        createOrUpdateTestDescriptors(autoUpdateItems)

        return result
    }
}

abstract class ResultStatus(open val value: Result<InstalledTestDescriptorModel?, MkException>) {
    data class AutoUpdated(override val value: Result<InstalledTestDescriptorModel?, MkException>) : ResultStatus(value)

    data class NoUpdates(override val value: Result<InstalledTestDescriptorModel?, MkException>) : ResultStatus(value)

    data class UpdatesAvailable(override val value: Result<InstalledTestDescriptorModel?, MkException>) : ResultStatus(value)
}
