package org.ooni.probe.domain.descriptors

import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorsUpdateState

class AcceptDescriptorUpdate(
    private val saveTestDescriptors: suspend (List<Descriptor>, SaveTestDescriptors.Mode) -> Unit,
    private val updateState: ((DescriptorsUpdateState) -> DescriptorsUpdateState) -> Unit,
) {
    suspend operator fun invoke(newDescriptor: Descriptor) {
        saveTestDescriptors(
            listOf(newDescriptor),
            SaveTestDescriptors.Mode.CreateOrUpdate,
        )
        updateState {
            it.copy(availableUpdates = it.availableUpdates - newDescriptor)
        }
    }
}
