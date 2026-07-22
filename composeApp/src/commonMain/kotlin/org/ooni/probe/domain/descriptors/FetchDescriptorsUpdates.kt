package org.ooni.probe.domain.descriptors

import co.touchlab.kermit.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.ooni.engine.Engine
import org.ooni.engine.models.Result
import org.ooni.passport.models.isOfflineFailure
import org.ooni.probe.data.models.DescriptorUpdateOperationState
import org.ooni.probe.data.models.DescriptorsUpdateState
import org.ooni.probe.data.models.Descriptor

class FetchDescriptorsUpdates(
    private val getLatestTestDescriptors: () -> Flow<List<Descriptor>>,
    private val fetchDescriptor: suspend (Descriptor.Id) -> Result<Descriptor?, Engine.MkException>,
    private val saveTestDescriptors: suspend (List<Descriptor>, SaveTestDescriptors.Mode) -> Unit,
    private val updateState: ((DescriptorsUpdateState) -> DescriptorsUpdateState) -> Unit,
) {
    suspend operator fun invoke(descriptorsProvided: List<Descriptor>): DescriptorUpdateOutcome {
        val descriptors = descriptorsProvided.ifEmpty { getLatestTestDescriptors().first() }

        updateState {
            DescriptorsUpdateState(
                operationState = DescriptorUpdateOperationState.FetchingUpdates,
            )
        }

        val fetchResults = coroutineScope {
            descriptors
                .chunked(MAX_FETCH_CONCURRENCY)
                .flatMap { descriptorsBatch ->
                    descriptorsBatch
                        .map { descriptor ->
                            async { descriptor to fetchDescriptor(descriptor.id) }
                        }.awaitAll()
                }
        }

        val minorUpdates = mutableListOf<Descriptor>()
        val updatesToReview = mutableListOf<Descriptor>()
        val autoUpdates = mutableListOf<Descriptor>()

        var successes = 0
        var networkFailures = 0
        var otherFailures = 0

        fetchResults.forEach { (descriptor, fetchResult) ->
            val newDescriptor = fetchResult
                .get()
                ?.copy(
                    autoUpdate = descriptor.autoUpdate,
                    dateInstalled = descriptor.dateInstalled,
                )
                ?: run {
                    val error = fetchResult.getError()
                    if (error.isOfflineFailure()) {
                        networkFailures++
                        // Expected while offline: no throwable, or every launch without a network
                        // fills the log with stack traces that look like crashes.
                        Logger.i("Skipping descriptor update ${descriptor.id.value}: no active network")
                    } else {
                        otherFailures++
                        Logger.w("Failed to fetch update", error)
                    }
                    return@forEach
                }

            successes++

            val newUpdate = newDescriptor.dateUpdated != null &&
                (
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

        return DescriptorUpdateOutcome(
            attempted = descriptors.size,
            successes = successes,
            networkFailures = networkFailures,
            otherFailures = otherFailures,
        )
    }

    companion object {
        private const val MAX_FETCH_CONCURRENCY = 4
    }
}
