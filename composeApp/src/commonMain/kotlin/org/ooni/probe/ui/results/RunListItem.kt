package org.ooni.probe.ui.results

import org.ooni.probe.data.models.MeasurementCounts
import org.ooni.probe.data.models.ResultListItem
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.RunModel
import org.ooni.probe.ui.shared.SelectableItem

data class RunListItem(
    val run: RunModel,
    val results: List<SelectableItem<ResultListItem>>,
    val isExpanded: Boolean = false,
) {
    val isDone get() = results.all { it.item.result.isDone }
    val selectedResultsIds get() = results.filter { it.isSelected }.map { it.item.result.idOrThrow }
    val measurementCounts = results.fold(MeasurementCounts()) { counts, result ->
        counts.add(result.item.measurementCounts)
    }

    fun changeSelection(
        item: ResultListItem,
        isSelected: Boolean,
    ) = copy(
        results = results.map { result ->
            result.copy(
                isSelected = if (result.item.result.idOrThrow == item.idOrThrow) {
                    isSelected
                } else {
                    result.isSelected
                },
            )
        },
    )

    fun changeAllSelections(isSelected: Boolean) =
        copy(results = results.map { result -> result.copy(isSelected = isSelected) })

    companion object {
        fun aggregateResults(
            results: List<ResultListItem>,
            expandedRunIds: List<RunModel.Id> = emptyList(),
            selectedResultsIds: List<ResultModel.Id> = emptyList(),
        ): List<RunListItem> =
            results
                .groupBy { it.result.runId ?: RunModel.Id.generateForSingleResult(it.result) }
                .mapNotNull { (_, resultsFromSameRun) ->
                    buildFromResults(resultsFromSameRun, expandedRunIds, selectedResultsIds)
                }

        private fun buildFromResults(
            results: List<ResultListItem>,
            expandedRunIds: List<RunModel.Id>,
            selectedResultsIds: List<ResultModel.Id>,
        ): RunListItem? {
            val first = results.minByOrNull { it.result.startTime } ?: return null
            val run = RunModel.fromResult(first)
            return RunListItem(
                run = run,
                results = results
                    .sortedBy { it.result.startTime }
                    .map { result ->
                        SelectableItem(
                            item = result,
                            isSelected = selectedResultsIds.contains(result.idOrThrow),
                        )
                    },
                isExpanded = expandedRunIds.contains(run.id)
            )
        }
    }
}

fun List<RunListItem>.updateWithNewResults(results: List<ResultListItem>): List<RunListItem> {
    val expandedRunIds = filter { it.isExpanded }. map { it.run.id }
    val selectedResultsIds = map { it.selectedResultsIds }.flatten()
    return RunListItem.aggregateResults(results, expandedRunIds, selectedResultsIds)
}
