package org.ooni.probe.domain.descriptors

import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.ResultFilter
import org.ooni.probe.data.models.toDescriptor
import org.ooni.probe.data.repositories.PreferenceRepository

class DeleteTestDescriptor(
    private val preferencesRepository: PreferenceRepository,
    private val deleteDescriptorByRunId: suspend (InstalledTestDescriptorModel.Id) -> Unit,
    private val deleteResultsByFilter: suspend (ResultFilter) -> Unit,
) {
    suspend operator fun invoke(testDescriptor: InstalledTestDescriptorModel) {
        val descriptor = testDescriptor.toDescriptor()
        preferencesRepository.removeDescriptorPreferences(descriptor)
        deleteResultsByFilter(ResultFilter(descriptors = listOf(descriptor)))
        deleteDescriptorByRunId(testDescriptor.id)
    }
}
