package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.ooni.engine.models.WebConnectivityCategory
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
            isEnabled = true,
        )
        preferencesRepository.setAreNetTestsEnabled(
            list = allTests,
            isAutoRun = true,
            isEnabled = true,
        )

        preferencesRepository.setValuesByKey(
            listOf(
                SettingsKey.FIRST_RUN to true,
                SettingsKey.MAX_RUNTIME_ENABLED to true,
                SettingsKey.MAX_RUNTIME to 90,
                SettingsKey.UPLOAD_RESULTS to true,
                SettingsKey.AUTOMATED_TESTING_WIFIONLY to true,
                SettingsKey.AUTOMATED_TESTING_CHARGING to true,
            ) +
                WebConnectivityCategory.entries
                    .mapNotNull { it.settingsKey }
                    .map { it to true },
        )
    }

    private suspend fun getAllNetTests() =
        getTestDescriptors()
            .first()
            .flatMap { descriptor ->
                descriptor.netTests.map { test -> descriptor to test }
            }
}
