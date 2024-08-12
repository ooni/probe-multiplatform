package org.ooni.probe.domain

import org.ooni.probe.data.models.InstalledTestDescriptorModel

class BootstrapTestDescriptors(
    private val getBootstrapTestDescriptors: suspend () -> List<InstalledTestDescriptorModel>,
    private val createOrIgnoreTestDescriptors: suspend (List<InstalledTestDescriptorModel>) -> Unit,
) {
    suspend operator fun invoke() {
        createOrIgnoreTestDescriptors(getBootstrapTestDescriptors())
    }
}
