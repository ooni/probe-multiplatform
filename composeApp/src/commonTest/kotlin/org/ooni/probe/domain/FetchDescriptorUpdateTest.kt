package org.ooni.probe.domain

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.ooni.engine.models.Success
import org.ooni.probe.data.models.DescriptorUpdateOperationState
import org.ooni.probe.data.models.DescriptorsUpdateState
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.domain.descriptors.FetchDescriptorsUpdates
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
            var saveDescriptors: List<InstalledTestDescriptorModel>? = null
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
                dateUpdated = LocalDateTime.now(),
            )
            var saveDescriptors: List<InstalledTestDescriptorModel>? = null
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
                dateUpdated = LocalDateTime.now(),
            )
            var saveDescriptors: List<InstalledTestDescriptorModel>? = null
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
}
