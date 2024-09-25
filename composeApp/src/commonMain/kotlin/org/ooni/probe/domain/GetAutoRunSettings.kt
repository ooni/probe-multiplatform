package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.ooni.probe.data.models.AutoRunParameters
import org.ooni.probe.data.models.SettingsKey

class GetAutoRunSettings(
    private val observeSettings: (List<SettingsKey>) -> Flow<Map<SettingsKey, Any?>>,
) {
    operator fun invoke(): Flow<AutoRunParameters> =
        observeSettings(
            listOf(
                SettingsKey.FIRST_RUN,
                SettingsKey.AUTOMATED_TESTING_ENABLED,
                SettingsKey.AUTOMATED_TESTING_WIFIONLY,
                SettingsKey.AUTOMATED_TESTING_CHARGING,
            ),
        ).map { preferences ->
            val firstRunDone = preferences[SettingsKey.FIRST_RUN] == false
            val enabled = preferences[SettingsKey.AUTOMATED_TESTING_ENABLED] == true
            if (firstRunDone && enabled) {
                AutoRunParameters.Enabled(
                    wifiOnly = preferences[SettingsKey.AUTOMATED_TESTING_WIFIONLY] == true,
                    onlyWhileCharging = preferences[SettingsKey.AUTOMATED_TESTING_CHARGING] == true,
                )
            } else {
                AutoRunParameters.Disabled
            }
        }
}
