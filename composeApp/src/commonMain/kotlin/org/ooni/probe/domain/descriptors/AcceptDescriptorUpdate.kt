package org.ooni.probe.domain.descriptors

import org.ooni.probe.data.models.DescriptorsUpdateState
import org.ooni.probe.data.models.InstalledTestDescriptorModel

class AcceptDescriptorUpdate(
    private val saveTestDescriptors: suspend (List<InstalledTestDescriptorModel>, SaveTestDescriptors.Mode) -> Unit,
    private val updateState: ((DescriptorsUpdateState) -> DescriptorsUpdateState) -> Unit,
) {
    suspend operator fun invoke(newDescriptor: InstalledTestDescriptorModel) {
        saveTestDescriptors(
            listOf(newDescriptor),
            SaveTestDescriptors.Mode.CreateOrUpdate,
        )
        updateState {
            it.copy(availableUpdates = it.availableUpdates - newDescriptor)
        }
    }
}
