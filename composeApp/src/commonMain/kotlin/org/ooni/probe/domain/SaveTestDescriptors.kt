package org.ooni.probe.domain

import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.toDescriptor
import org.ooni.probe.data.repositories.PreferenceRepository

class SaveTestDescriptors(
    private val preferencesRepository: PreferenceRepository,
    private val createOrIgnoreTestDescriptors: suspend (List<InstalledTestDescriptorModel>) -> Unit,
) {
    suspend operator fun invoke(models: List<Pair<InstalledTestDescriptorModel, List<NetTest>>>) {
        models.forEach { (model, tests) ->
            preferencesRepository.setAreNetTestsEnabled(
                list = tests.map { test ->
                    model.toDescriptor() to test
                },
                isAutoRun = true,
                isEnabled = true,
            )
        }
        createOrIgnoreTestDescriptors(models.map { it.first })
    }
}
