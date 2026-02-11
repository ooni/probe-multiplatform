package org.ooni.probe.domain.descriptors

import org.ooni.probe.data.models.Descriptor

class BootstrapTestDescriptors(
    private val getBootstrapTestDescriptors: suspend () -> List<Descriptor>,
    private val saveTestDescriptors: suspend (List<Descriptor>, SaveTestDescriptors.Mode) -> Unit,
) {
    suspend operator fun invoke() {
        val descriptors = getBootstrapTestDescriptors()
        saveTestDescriptors(descriptors, SaveTestDescriptors.Mode.CreateOrIgnore)
    }
}
