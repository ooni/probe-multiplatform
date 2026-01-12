package org.ooni.probe.domain.descriptors

import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorsUpdateState

class RejectDescriptorUpdate(
    private val updateDescriptorRejectedRevision: suspend (Descriptor.Id, Long?) -> Unit,
    private val updateState: ((DescriptorsUpdateState) -> DescriptorsUpdateState) -> Unit,
) {
    suspend operator fun invoke(newDescriptor: Descriptor) {
        updateDescriptorRejectedRevision(newDescriptor.id, newDescriptor.revision)
        updateState { it.copy(availableUpdates = it.availableUpdates - newDescriptor) }
    }
}
