package org.ooni.probe.domain.descriptors

import co.touchlab.kermit.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.ooni.engine.Engine
import org.ooni.engine.models.Result
import org.ooni.probe.data.models.DescriptorsUpdateState
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.DescriptorUpdateOperationState

class FetchDescriptorsUpdates(
    private val fetchDescriptor: suspend (descriptorId: String) -> Result<InstalledTestDescriptorModel?, Engine.MkException>,
    private val saveTestDescriptors: suspend (List<InstalledTestDescriptorModel>, SaveTestDescriptors.Mode) -> Unit,
    private val updateState: ((DescriptorsUpdateState) -> DescriptorsUpdateState) -> Unit,
) {
    suspend operator fun invoke(descriptors: List<InstalledTestDescriptorModel>) {
        if (descriptors.isEmpty()) {
            Logger.i("Skipping, no descriptors to update")
        }

        updateState {
            DescriptorsUpdateState(
                operationState = DescriptorUpdateOperationState.FetchingUpdates,
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
                if (newDescriptor.revision == descriptor.rejectedRevision) {
                    return@forEach // User already rejected that update
                } else if (descriptor.autoUpdate) {
                    autoUpdates += newDescriptor
                } else {
                    updatesToReview += newDescriptor
                }
            } else {
                minorUpdates += newDescriptor
            }
        }

        saveTestDescriptors(minorUpdates + autoUpdates, SaveTestDescriptors.Mode.CreateOrUpdate)

        updateState {
            DescriptorsUpdateState(
                availableUpdates = updatesToReview,
                autoUpdated = autoUpdates,
                operationState = if (updatesToReview.isNotEmpty()) {
                    DescriptorUpdateOperationState.ReviewNecessaryNotice
                } else {
                    DescriptorUpdateOperationState.Idle
                },
            )
        }
    }
}
