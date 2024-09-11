package org.ooni.probe.domain

import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.toDescriptor
import org.ooni.probe.data.repositories.PreferenceRepository

class DeleteTestDescriptor(
    private val preferencesRepository: PreferenceRepository,
    private val deleteByRunId: suspend (InstalledTestDescriptorModel.Id) -> Unit,
) {
    suspend operator fun invoke(testDescriptor: InstalledTestDescriptorModel) {
        preferencesRepository.removeDescriptorPreferences(
            descriptor = testDescriptor.toDescriptor(),
        )
        deleteByRunId(testDescriptor.id)
    }
}
