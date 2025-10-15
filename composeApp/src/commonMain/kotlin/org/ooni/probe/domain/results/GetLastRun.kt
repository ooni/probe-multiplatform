package org.ooni.probe.domain.results

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.ResultWithNetworkAndAggregates
import org.ooni.probe.data.models.Run
import org.ooni.probe.data.models.SettingsKey
import kotlin.time.Duration.Companion.hours

/*
 * Estimate what results belong to a single run. The criteria are:
 * - No duplicated descriptors inside a single run
 * - Start time has to be inside a MAX_RUN_DURATION window
 * - Does not contain a result already dismissed by the user
 */
class GetLastRun(
    private val getLastResults: (Int) -> Flow<List<ResultWithNetworkAndAggregates>>,
    private val getPreference: (SettingsKey) -> Flow<Any?>,
) {
    operator fun invoke(): Flow<Run?> =
        combine(
            getLastResults(MAX_RESULTS_IN_RUN),
            getPreference(SettingsKey.LAST_RUN_DISMISSED),
        ) { lastResults, lastRunDismissed ->
            val lastDoneResults = lastResults.filter { it.result.isDone }
            val lastDismissedResultId = (lastRunDismissed as? Long)?.let(ResultModel::Id)
            val lastRunResults = lastDoneResults.getLastRunResults(lastDismissedResultId)
            if (lastRunResults.isEmpty()) {
                null
            } else {
                Run(results = lastRunResults)
            }
        }

    private fun List<ResultWithNetworkAndAggregates>.getLastRunResults(
        lastDismissedResultId: ResultModel.Id?,
    ): List<ResultWithNetworkAndAggregates> {
        (1..size).forEach { index ->
            val list = take(index)
            if (
                list.resultsExceedMaxRunDuration() ||
                list.hasDuplicatedDescriptors() ||
                list.any { it.result.id == lastDismissedResultId }
            ) {
                return take(index - 1)
            }
        }
        return this
    }

    private fun List<ResultWithNetworkAndAggregates>.resultsExceedMaxRunDuration(): Boolean {
        if (size <= 1) return false
        val firstStartTime = first().result.startTime.toInstant(TimeZone.UTC)
        val lastStartTime = last().result.startTime.toInstant(TimeZone.UTC)
        return firstStartTime - lastStartTime > MAX_RUN_DURATION
    }

    private fun List<ResultWithNetworkAndAggregates>.hasDuplicatedDescriptors() =
        groupBy { it.result.descriptorKey?.id ?: it.result.descriptorName }
            .any { it.value.size > 1 }

    companion object Companion {
        private const val MAX_RESULTS_IN_RUN = 50
        private val MAX_RUN_DURATION = 1.hours
    }
}
