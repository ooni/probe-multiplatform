package org.ooni.probe.domain.descriptors

import org.ooni.probe.data.models.DescriptorsUpdateState
import org.ooni.probe.data.models.InstalledTestDescriptorModel

class RejectDescriptorUpdate(
    private val updateDescriptorRejectedRevision: suspend (InstalledTestDescriptorModel.Id, Long?) -> Unit,
    private val updateState: ((DescriptorsUpdateState) -> DescriptorsUpdateState) -> Unit,
) {
    suspend operator fun invoke(newDescriptor: InstalledTestDescriptorModel) {
        updateDescriptorRejectedRevision(newDescriptor.id, newDescriptor.revision)
        updateState { it.copy(availableUpdates = it.availableUpdates - newDescriptor) }
    }
}
