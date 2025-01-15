package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import org.ooni.engine.Engine
import org.ooni.engine.models.Result
import org.ooni.probe.data.models.DescriptorUpdatesStatus
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.UpdateStatusType

class FetchDescriptorUpdate(
    private val fetchDescriptor: suspend (descriptorId: String) -> Result<InstalledTestDescriptorModel?, Engine.MkException>,
    private val saveTestDescriptors: suspend (List<InstalledTestDescriptorModel>, SaveTestDescriptors.Mode) -> Unit,
    private val listInstalledTestDescriptors: () -> Flow<List<InstalledTestDescriptorModel>>,
) {
    private val status = MutableStateFlow(DescriptorUpdatesStatus())

    fun observeStatus() = status.asStateFlow()

    suspend fun invoke(descriptors: List<InstalledTestDescriptorModel>) {
        status.update { _ ->
            DescriptorUpdatesStatus(
                refreshType = UpdateStatusType.FetchingUpdates,
            )
        }
        val fetchResults = coroutineScope {
            descriptors.map { descriptor ->
                async { descriptor to fetchDescriptor(descriptor.id.value) }
            }.awaitAll()
        }

        val minorUpdates = mutableListOf<InstalledTestDescriptorModel>()
        val updatesToReview = mutableListOf<InstalledTestDescriptorModel>()
        val autoUpdates = mutableListOf<InstalledTestDescriptorModel>()

        fetchResults.forEach { (descriptor, fetchResult) ->
            val newDescriptor = fetchResult.get()
                ?.copy(autoUpdate = descriptor.autoUpdate)
                ?: run {
                    Logger.w("Failed to fetch update", fetchResult.getError())
                    return@forEach
                }

            val newUpdate = newDescriptor.dateUpdated != null && (
                descriptor.dateUpdated == null || descriptor.dateUpdated < newDescriptor.dateUpdated
            )
            if (!newUpdate) return@forEach

            if (newDescriptor.revision > descriptor.revision) {
                // Major update
                if (descriptor.autoUpdate) {
                    autoUpdates += newDescriptor
                } else {
                    updatesToReview += newDescriptor
                }
            } else {
                minorUpdates += newDescriptor
            }
        }

        saveTestDescriptors(minorUpdates + autoUpdates, SaveTestDescriptors.Mode.CreateOrUpdate)

        status.update { _ ->
            DescriptorUpdatesStatus(
                availableUpdates = updatesToReview,
                autoUpdated = autoUpdates,
                refreshType = if (updatesToReview.isNotEmpty()) {
                    UpdateStatusType.ReviewLink
                } else {
                    UpdateStatusType.None
                },
            )
        }
    }

    suspend operator fun invoke() {
        val items = listInstalledTestDescriptors().first()
        if (items.isNotEmpty()) {
            invoke(items)
        }
    }

    fun cancelUpdates(descriptors: List<InstalledTestDescriptorModel>) {
        status.update { currentItems ->
            currentItems.copy(
                availableUpdates = currentItems.availableUpdates - descriptors.toSet(),
                rejectedUpdates = currentItems.availableUpdates + descriptors,
                refreshType = UpdateStatusType.None,
            )
        }
    }

    fun reviewUpdates(itemsForReview: List<InstalledTestDescriptorModel>) {
        status.update { currentItems ->
            currentItems.copy(
                reviewUpdates = itemsForReview,
                refreshType = UpdateStatusType.None,
            )
        }
    }

    fun markAsUpdated(items: List<InstalledTestDescriptorModel>) {
        status.update { status ->
            status.copy(
                availableUpdates = status.availableUpdates - items.toSet(),
                rejectedUpdates = status.rejectedUpdates - items.toSet(),
                reviewUpdates = status.reviewUpdates - items.toSet(),
            )
        }
    }
}
