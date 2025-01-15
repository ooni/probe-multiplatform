package org.ooni.probe.domain

import org.ooni.probe.data.models.InstalledTestDescriptorModel

class BootstrapTestDescriptors(
    private val getBootstrapTestDescriptors: suspend () -> List<InstalledTestDescriptorModel>,
    private val saveTestDescriptors: suspend (List<InstalledTestDescriptorModel>, SaveTestDescriptors.Mode) -> Unit,
) {
    suspend operator fun invoke() {
        val descriptors = getBootstrapTestDescriptors()
        saveTestDescriptors(descriptors, SaveTestDescriptors.Mode.CreateOrIgnore)
    }
}
