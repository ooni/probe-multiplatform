package org.ooni.probe.domain.results

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.ooni.probe.data.models.ResultWithNetworkAndAggregates
import org.ooni.probe.data.models.RunModel
import org.ooni.probe.data.models.RunSummary
import org.ooni.probe.data.models.SettingsKey

/*
 * Get last run if it wasn't dismissed by the user.
 * If there isn't a last run (legacy), create one from the last result.
 */
class GetLastRun(
    private val getLastRunResults: () -> Flow<List<ResultWithNetworkAndAggregates>>,
    private val getLastResult: () -> Flow<ResultWithNetworkAndAggregates?>,
    private val getPreference: (SettingsKey) -> Flow<Any?>,
) {
    operator fun invoke(): Flow<RunSummary?> =
        combine(
            getLastRunResults(),
            getLastResult(),
            getPreference(SettingsKey.LAST_RUN_DISMISSED),
        ) { lastRunResults, lastResult, lastRunDismissed ->
            val runResults = if (lastRunResults.any()) lastRunResults else listOfNotNull(lastResult)
            val runSummary = RunSummary.fromResults(runResults) ?: return@combine null
            val lastDismissedRunId = (lastRunDismissed as? String)?.let(RunModel::Id)

            if (runSummary.run.id == lastDismissedRunId) {
                null
            } else {
                runSummary
            }
        }
}
