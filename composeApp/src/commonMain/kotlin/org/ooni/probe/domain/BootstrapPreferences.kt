package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository

class BootstrapPreferences(
    private val preferencesRepository: PreferenceRepository,
    private val getTestDescriptors: () -> Flow<List<Descriptor>>,
) {
    suspend operator fun invoke() {
        if (preferencesRepository.getValueByKey(SettingsKey.FIRST_RUN).first() != null) return

        val allTests = getAllNetTests()
        preferencesRepository.setAreNetTestsEnabled(
            list = allTests,
            isAutoRun = false,
        )
        preferencesRepository.setAreNetTestsEnabled(
            list = allTests,
            isAutoRun = true,
        )

        preferencesRepository.setValuesByKey(
            listOf(
                SettingsKey.FIRST_RUN to true,
                SettingsKey.UPLOAD_RESULTS to true,
                SettingsKey.AUTOMATED_TESTING_WIFIONLY to true,
                SettingsKey.AUTOMATED_TESTING_CHARGING to true,
            ) +
                organizationPreferenceDefaults(),
        )
    }

    private suspend fun getAllNetTests() =
        getTestDescriptors()
            .first()
            .flatMap { descriptor ->
                descriptor.netTests.map { test -> descriptor to test }
            }
}
