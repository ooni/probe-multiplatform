package org.ooni.probe.domain.descriptors

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.ooni.engine.Engine.MkException
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.passport.models.PassportException
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorUpdateOperationState
import org.ooni.probe.data.models.DescriptorsUpdateState
import org.ooni.probe.shared.now
import org.ooni.probe.shared.toLocalDateTime
import org.ooni.testing.factories.DescriptorFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class FetchDescriptorUpdateTest {
    @Test
    fun noUpdate() =
        runTest {
            val oldDescriptor = DescriptorFactory.buildInstalledModel(autoUpdate = true)
            var saveDescriptors: List<Descriptor>? = null
            var state: DescriptorsUpdateState? = null
            val subject = FetchDescriptorsUpdates(
                getLatestTestDescriptors = { emptyFlow() },
                fetchDescriptor = { Success(oldDescriptor) },
                saveTestDescriptors = { list, _ -> saveDescriptors = list },
                updateState = { state = it(state ?: DescriptorsUpdateState()) },
            )

            subject(listOf(oldDescriptor))

            assertEquals(0, state!!.autoUpdated.size)
            assertEquals(DescriptorUpdateOperationState.Idle, state.operationState)
            assertTrue(saveDescriptors.isNullOrEmpty())
        }

    @Test
    fun autoUpdate() =
        runTest {
            val oldDescriptor = DescriptorFactory.buildInstalledModel(
                autoUpdate = true,
                dateUpdated = Clock.System
                    .now()
                    .minus(1.days)
                    .toLocalDateTime(),
            )
            val newDescriptor = oldDescriptor.copy(
                revision = 2,
                dateUpdated = LocalDateTime.Companion.now(),
            )
            var saveDescriptors: List<Descriptor>? = null
            var state: DescriptorsUpdateState? = null
            val subject = FetchDescriptorsUpdates(
                getLatestTestDescriptors = { emptyFlow() },
                fetchDescriptor = { Success(newDescriptor) },
                saveTestDescriptors = { list, _ -> saveDescriptors = list },
                updateState = { state = it(state ?: DescriptorsUpdateState()) },
            )

            subject(listOf(oldDescriptor))

            assertEquals(1, state!!.autoUpdated.size)
            assertEquals(DescriptorUpdateOperationState.Idle, state.operationState)
            assertEquals(listOf(newDescriptor), saveDescriptors)
        }

    @Test
    fun minorUpdate() =
        runTest {
            val oldDescriptor = DescriptorFactory.buildInstalledModel(
                autoUpdate = false,
                dateUpdated = Clock.System
                    .now()
                    .minus(1.days)
                    .toLocalDateTime(),
            )
            val newDescriptor = oldDescriptor.copy(
                dateUpdated = LocalDateTime.Companion.now(),
            )
            var saveDescriptors: List<Descriptor>? = null
            var state: DescriptorsUpdateState? = null
            val subject = FetchDescriptorsUpdates(
                getLatestTestDescriptors = { emptyFlow() },
                fetchDescriptor = { Success(newDescriptor) },
                saveTestDescriptors = { list, _ -> saveDescriptors = list },
                updateState = { state = it(state ?: DescriptorsUpdateState()) },
            )

            subject(listOf(oldDescriptor))

            assertEquals(0, state!!.availableUpdates.size)
            assertEquals(0, state.autoUpdated.size)
            assertEquals(DescriptorUpdateOperationState.Idle, state.operationState)
            assertEquals(listOf(newDescriptor), saveDescriptors)
        }

    @Test
    fun outcomeCountsSuccesses() =
        runTest {
            val descriptor = DescriptorFactory.buildInstalledModel(autoUpdate = true)
            val subject = subject(fetchDescriptor = { Success(descriptor) })

            val outcome = subject(listOf(descriptor, descriptor))

            assertEquals(
                DescriptorUpdateOutcome(attempted = 2, successes = 2),
                outcome,
            )
        }

    @Test
    fun outcomeCountsOfflineFailuresSeparatelyFromOtherFailures() =
        runTest {
            val descriptor = DescriptorFactory.buildInstalledModel(autoUpdate = true)
            val offline = subject(
                fetchDescriptor = {
                    Failure(MkException(PassportException.Offline("no active network")))
                },
            )
            val serverError = subject(
                fetchDescriptor = {
                    Failure(MkException(PassportException.HttpClientError("HTTP 500")))
                },
            )

            assertEquals(
                DescriptorUpdateOutcome(attempted = 2, networkFailures = 2),
                offline(listOf(descriptor, descriptor)),
            )
            assertEquals(
                DescriptorUpdateOutcome(attempted = 2, otherFailures = 2),
                serverError(listOf(descriptor, descriptor)),
            )
        }

    /**
     * An empty argument means "every installed descriptor", so the count has to come from the
     * resolved list - otherwise the worker sees zero attempts and never retries.
     */
    @Test
    fun outcomeCountsResolvedDescriptorsWhenNoneWereProvided() =
        runTest {
            val descriptor = DescriptorFactory.buildInstalledModel(autoUpdate = true)
            val subject = subject(
                getLatestTestDescriptors = { flowOf(listOf(descriptor, descriptor, descriptor)) },
                fetchDescriptor = {
                    Failure(MkException(PassportException.Offline("no active network")))
                },
            )

            val outcome = subject(emptyList())

            assertEquals(3, outcome.attempted)
            assertEquals(3, outcome.networkFailures)
            assertTrue(outcome.shouldRetry(runAttemptCount = 0))
        }

    private fun subject(
        fetchDescriptor: suspend (Descriptor.Id) -> Result<Descriptor?, MkException>,
        getLatestTestDescriptors: () -> Flow<List<Descriptor>> = { emptyFlow() },
    ) = FetchDescriptorsUpdates(
        getLatestTestDescriptors = getLatestTestDescriptors,
        fetchDescriptor = fetchDescriptor,
        saveTestDescriptors = { _, _ -> },
        updateState = { },
    )
}
