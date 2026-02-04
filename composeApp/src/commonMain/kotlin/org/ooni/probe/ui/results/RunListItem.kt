package org.ooni.probe.ui.results

import org.ooni.probe.data.models.ResultListItem
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.RunModel
import org.ooni.probe.ui.shared.SelectableItem

data class RunListItem(
    val run: RunModel,
    val results: List<SelectableItem<ResultListItem>>,
) {
    val isDone get() = results.all { it.item.result.isDone }
    val selectedResultsIds get() = results.filter { it.isSelected }.map { it.item.result.idOrThrow }

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

    fun changeAllSelections(isSelected: Boolean) = copy(results = results.map { result -> result.copy(isSelected = isSelected) })

    companion object {
        fun aggregateResults(
            results: List<ResultListItem>,
            selectedResultsIds: List<ResultModel.Id> = emptyList(),
        ): List<RunListItem> =
            results
                .groupBy { it.result.runId ?: RunModel.Id.generateForSingleResult(it.result) }
                .mapNotNull { (_, resultsFromSameRun) ->
                    buildFromResults(resultsFromSameRun, selectedResultsIds)
                }

        private fun buildFromResults(
            results: List<ResultListItem>,
            selectedResultsIds: List<ResultModel.Id> = emptyList(),
        ): RunListItem? {
            val first = results.minByOrNull { it.result.startTime } ?: return null
            return RunListItem(
                run = RunModel.fromResultAndNetwork(first.result, first.network),
                results = results
                    .sortedBy { it.result.startTime }
                    .map { result ->
                        SelectableItem(
                            item = result,
                            isSelected = selectedResultsIds.contains(result.idOrThrow),
                        )
                    },
            )
        }
    }
}

fun List<RunListItem>.updateWithNewResults(results: List<ResultListItem>): List<RunListItem> {
    val selectedResultsIds = map { it.selectedResultsIds }.flatten()
    return RunListItem.aggregateResults(results, selectedResultsIds)
}
