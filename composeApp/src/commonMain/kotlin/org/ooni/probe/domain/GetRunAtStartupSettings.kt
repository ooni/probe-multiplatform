package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.ooni.probe.data.models.SettingsKey

class GetRunAtStartupSettings(
    private val getPreference: (SettingsKey) -> Flow<Any?>,
) {
    operator fun invoke(): Flow<Boolean> =
        getPreference(SettingsKey.RUN_AT_STARTUP)
            .map { it == true }
            .distinctUntilChanged()
}
