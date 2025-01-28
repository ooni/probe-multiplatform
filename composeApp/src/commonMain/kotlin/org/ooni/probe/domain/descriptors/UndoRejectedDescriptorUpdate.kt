package org.ooni.probe.domain.descriptors

import org.ooni.probe.data.models.InstalledTestDescriptorModel

class UndoRejectedDescriptorUpdate(
    private val updateDescriptorRejectedRevision: suspend (InstalledTestDescriptorModel.Id, Long?) -> Unit,
) {
    suspend operator fun invoke(id: InstalledTestDescriptorModel.Id) {
        updateDescriptorRejectedRevision(id, null)
    }
}
