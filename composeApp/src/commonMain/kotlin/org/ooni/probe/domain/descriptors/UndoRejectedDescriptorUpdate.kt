package org.ooni.probe.domain.descriptors

import org.ooni.probe.data.models.Descriptor

class UndoRejectedDescriptorUpdate(
    private val updateDescriptorRejectedRevision: suspend (Descriptor.Id, Long?) -> Unit,
) {
    suspend operator fun invoke(id: Descriptor.Id) {
        updateDescriptorRejectedRevision(id, null)
    }
}
