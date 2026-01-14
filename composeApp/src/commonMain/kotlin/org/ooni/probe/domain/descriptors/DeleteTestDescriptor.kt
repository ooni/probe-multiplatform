package org.ooni.probe.domain.descriptors

import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.ResultFilter
import org.ooni.probe.data.models.toDescriptorItem
import org.ooni.probe.data.repositories.PreferenceRepository

class DeleteTestDescriptor(
    private val preferencesRepository: PreferenceRepository,
    private val deleteDescriptorByRunId: suspend (Descriptor.Id) -> Unit,
    private val deleteResultsByFilter: suspend (ResultFilter) -> Unit,
) {
    suspend operator fun invoke(testDescriptor: Descriptor) {
        val descriptor = testDescriptor.toDescriptorItem()
        preferencesRepository.removeDescriptorPreferences(descriptor)
        deleteResultsByFilter(ResultFilter(descriptors = listOf(descriptor)))
        deleteDescriptorByRunId(testDescriptor.id)
    }
}
