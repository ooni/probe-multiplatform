package org.ooni.probe.domain

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import org.ooni.engine.Engine.MkException
import org.ooni.engine.models.Result
import org.ooni.probe.data.models.DescriptorUpdatesStatus
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.UpdateStatusType

class FetchDescriptorUpdate(
    private val fetchDescriptor: suspend (descriptorId: String) -> Result<InstalledTestDescriptorModel?, MkException>,
    private val createOrUpdateTestDescriptors: suspend (Set<InstalledTestDescriptorModel>) -> Unit,
    private val listInstalledTestDescriptors: () -> Flow<List<InstalledTestDescriptorModel>>,
) {
    private val availableUpdates = MutableStateFlow(DescriptorUpdatesStatus())

    suspend fun invoke(
        descriptors: List<InstalledTestDescriptorModel>,
    ): MutableMap<ResultStatus, MutableList<Result<InstalledTestDescriptorModel?, MkException>>> {
        availableUpdates.update { _ ->
            DescriptorUpdatesStatus(
                refreshType = UpdateStatusType.FetchingUpdates,
            )
        }
        val response = coroutineScope {
            descriptors.map { descriptor ->
                async {
                    Pair(descriptor, fetchDescriptor(descriptor.id.value.toString()))
                }
            }.awaitAll()
        }
        val autoUpdateItems = mutableSetOf<InstalledTestDescriptorModel>()
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
        val updatesAvailable: List<InstalledTestDescriptorModel> = resultsMap[ResultStatus.UpdatesAvailable]?.mapNotNull { result ->
            result.get()
        }.orEmpty()
        val autoUpdated: List<InstalledTestDescriptorModel> = resultsMap[ResultStatus.AutoUpdated]?.mapNotNull { result ->
            result.get()
        }.orEmpty()
        availableUpdates.update { _ ->
            DescriptorUpdatesStatus(
                availableUpdates = updatesAvailable.toSet(),
                autoUpdated = autoUpdated.toSet(),
                refreshType = if (updatesAvailable.isNotEmpty()) UpdateStatusType.ReviewLink else UpdateStatusType.None,
            )
        }
        return resultsMap
    }

    suspend operator fun invoke() {
        listInstalledTestDescriptors.invoke().first().let { items ->
            if (items.isNotEmpty()) {
                invoke(items)
            }
        }
    }

    fun cancelUpdates(descriptors: Set<InstalledTestDescriptorModel>) {
        removeUpdates(descriptors)
        availableUpdates.update {
                currentItems ->
            currentItems.copy(
                availableUpdates = currentItems.availableUpdates - descriptors,
                rejectedUpdates = currentItems.availableUpdates + descriptors,
                refreshType = UpdateStatusType.None,
            )
        }
    }

    fun reviewUpdates(itemsForReview: List<InstalledTestDescriptorModel>) {
        availableUpdates.update {
                currentItems ->
            currentItems.copy(
                reviewUpdates = itemsForReview.toSet(),
            )
        }
    }

    fun observeAvailableUpdatesState() = availableUpdates.asStateFlow()

    fun removeUpdates(items: Set<InstalledTestDescriptorModel>) {
        availableUpdates.update {
                currentItems ->
            currentItems.copy(
                reviewUpdates = currentItems.reviewUpdates - items,
                availableUpdates = currentItems.availableUpdates - items,
                refreshType = UpdateStatusType.None,
            )
        }
    }
}

enum class ResultStatus {
    AutoUpdated,
    NoUpdates,
    UpdatesAvailable,
}
