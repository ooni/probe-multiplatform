package org.ooni.probe.domain

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import org.ooni.engine.models.Success
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.UpdateStatusType
import org.ooni.probe.shared.now
import org.ooni.probe.shared.toLocalDateTime
import org.ooni.testing.factories.DescriptorFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class FetchDescriptorUpdateTest {
    @Test
    fun noUpdate() =
        runTest {
            val oldDescriptor = DescriptorFactory.buildInstalledModel(autoUpdate = true)
            var saveDescriptors: List<InstalledTestDescriptorModel>? = null
            val subject = FetchDescriptorUpdate(
                fetchDescriptor = { Success(oldDescriptor) },
                saveTestDescriptors = { list, _ -> saveDescriptors = list },
                listInstalledTestDescriptors = { flowOf(listOf(oldDescriptor)) },
            )

            subject()

            val state = subject.observeStatus().value

            assertEquals(0, state.autoUpdated.size)
            assertEquals(UpdateStatusType.None, state.refreshType)
            assertTrue(saveDescriptors.isNullOrEmpty())
        }

    @Test
    fun autoUpdate() =
        runTest {
            val oldDescriptor = DescriptorFactory.buildInstalledModel(
                autoUpdate = true,
                dateUpdated = Clock.System.now().minus(1.days).toLocalDateTime(),
            )
            val newDescriptor = oldDescriptor.copy(
                revision = 2,
                dateUpdated = LocalDateTime.now(),
            )
            var saveDescriptors: List<InstalledTestDescriptorModel>? = null
            val subject = FetchDescriptorUpdate(
                fetchDescriptor = { Success(newDescriptor) },
                saveTestDescriptors = { list, _ -> saveDescriptors = list },
                listInstalledTestDescriptors = { flowOf(listOf(oldDescriptor)) },
            )

            subject()

            val state = subject.observeStatus().value

            assertEquals(1, state.autoUpdated.size)
            assertEquals(UpdateStatusType.None, state.refreshType)
            assertEquals(listOf(newDescriptor), saveDescriptors)
        }

    @Test
    fun minorUpdate() =
        runTest {
            val oldDescriptor = DescriptorFactory.buildInstalledModel(
                autoUpdate = false,
                dateUpdated = Clock.System.now().minus(1.days).toLocalDateTime(),
            )
            val newDescriptor = oldDescriptor.copy(
                dateUpdated = LocalDateTime.now(),
            )
            var saveDescriptors: List<InstalledTestDescriptorModel>? = null
            val subject = FetchDescriptorUpdate(
                fetchDescriptor = { Success(newDescriptor) },
                saveTestDescriptors = { list, _ -> saveDescriptors = list },
                listInstalledTestDescriptors = { flowOf(listOf(oldDescriptor)) },
            )

            subject()

            val state = subject.observeStatus().value

            assertEquals(0, state.availableUpdates.size)
            assertEquals(0, state.autoUpdated.size)
            assertEquals(UpdateStatusType.None, state.refreshType)
            assertEquals(listOf(newDescriptor), saveDescriptors)
        }

    @Test
    fun updatesAvailableAndReview() =
        runTest {
            val oldDescriptor = DescriptorFactory.buildInstalledModel(
                autoUpdate = false,
                dateUpdated = Clock.System.now().minus(1.days).toLocalDateTime(),
            )
            val newDescriptor = oldDescriptor.copy(
                revision = 2,
                dateUpdated = LocalDateTime.now(),
            )
            var saveDescriptors: List<InstalledTestDescriptorModel>? = null
            val subject = FetchDescriptorUpdate(
                fetchDescriptor = { Success(newDescriptor) },
                saveTestDescriptors = { list, _ -> saveDescriptors = list },
                listInstalledTestDescriptors = { flowOf(listOf(oldDescriptor)) },
            )

            subject()

            val state = subject.observeStatus().value

            assertEquals(1, state.availableUpdates.size)
            assertEquals(0, state.reviewUpdates.size)
            assertEquals(UpdateStatusType.ReviewLink, state.refreshType)
            assertTrue(saveDescriptors.isNullOrEmpty())

            subject.reviewUpdates(listOf(newDescriptor))

            val reviewState = subject.observeStatus().value

            assertEquals(1, reviewState.reviewUpdates.size)
            assertEquals(UpdateStatusType.None, reviewState.refreshType)
        }
}
