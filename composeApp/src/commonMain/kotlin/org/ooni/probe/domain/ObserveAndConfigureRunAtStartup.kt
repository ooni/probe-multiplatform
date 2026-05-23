package org.ooni.probe.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.ooni.probe.data.models.AutoRunParameters
import org.ooni.probe.data.models.SettingsKey
import kotlin.coroutines.CoroutineContext

class ObserveAndConfigureRunAtStartup(
    private val backgroundContext: CoroutineContext,
    private val getRunAtStartupSettings: () -> Flow<Boolean>,
    private val getAutoRunSettings: () -> Flow<AutoRunParameters>,
    private val setPreference: suspend (SettingsKey, Any?) -> Unit,
    private val configureRunAtStartup: suspend (Boolean) -> Unit,
) {
    operator fun invoke() {
        val scope = CoroutineScope(backgroundContext)

        // Enabling automated testing forces run-at-startup on. We only ever
        // write RUN_AT_STARTUP here, never any AUTOMATED_TESTING_* preference.
        getAutoRunSettings()
            .map { it is AutoRunParameters.Enabled }
            .distinctUntilChanged()
            .filter { autoRunEnabled -> autoRunEnabled }
            .onEach { setPreference(SettingsKey.RUN_AT_STARTUP, true) }
            .launchIn(scope)

        // Keep the OS login item in sync with the RUN_AT_STARTUP preference.
        getRunAtStartupSettings()
            .onEach { configureRunAtStartup(it) }
            .launchIn(scope)
    }
}
