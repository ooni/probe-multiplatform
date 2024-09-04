package org.ooni.probe.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.ooni.probe.data.models.AutoRunParameters
import org.ooni.probe.data.models.SettingsKey

class ObserveAndConfigureAutoRun(
    private val backgroundDispatcher: CoroutineDispatcher,
    private val observeSettings: (List<SettingsKey>) -> Flow<Map<SettingsKey, Any?>>,
    private val configureAutoRun: suspend (AutoRunParameters) -> Unit,
) {
    operator fun invoke() =
        observeParameters()
            .onEach { configureAutoRun(it) }
            .launchIn(CoroutineScope(backgroundDispatcher))

    private fun observeParameters() =
        observeSettings(
            listOf(
                SettingsKey.AUTOMATED_TESTING_ENABLED,
                SettingsKey.AUTOMATED_TESTING_WIFIONLY,
                SettingsKey.AUTOMATED_TESTING_CHARGING,
            ),
        ).map { preferences ->
            val enabled = preferences[SettingsKey.AUTOMATED_TESTING_ENABLED] == true
            if (enabled) {
                AutoRunParameters.Enabled(
                    wifiOnly = preferences[SettingsKey.AUTOMATED_TESTING_WIFIONLY] == true,
                    onlyWhileCharging = preferences[SettingsKey.AUTOMATED_TESTING_CHARGING] == true,
                )
            } else {
                AutoRunParameters.Disabled
            }
        }
}
