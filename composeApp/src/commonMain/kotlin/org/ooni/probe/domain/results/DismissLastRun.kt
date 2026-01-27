package org.ooni.probe.domain.results

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.ooni.probe.data.models.RunSummary
import org.ooni.probe.data.models.SettingsKey

class DismissLastRun(
    private val getLastRun: () -> Flow<RunSummary?>,
    private val setPreference: suspend (SettingsKey, Any) -> Unit,
) {
    suspend operator fun invoke() {
        val lastRun = getLastRun().first() ?: return
        setPreference(SettingsKey.LAST_RUN_DISMISSED, lastRun.run.id.value)
    }
}
