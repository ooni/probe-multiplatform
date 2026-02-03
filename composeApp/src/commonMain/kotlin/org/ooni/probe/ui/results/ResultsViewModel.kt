package org.ooni.probe.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.NetworkModel
import org.ooni.probe.data.models.ResultFilter
import org.ooni.probe.data.models.ResultListItem
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.ResultsStats

class ResultsViewModel(
    goToResult: (ResultModel.Id) -> Unit,
    goToUpload: () -> Unit,
    getResults: (ResultFilter) -> Flow<List<ResultListItem>>,
    getResultsStats: (ResultFilter) -> Flow<ResultsStats>,
    getDescriptors: () -> Flow<List<Descriptor>>,
    getNetworks: () -> Flow<List<NetworkModel>>,
    deleteResultsByFilter: suspend (ResultFilter) -> Unit,
    markJustFinishedTestAsSeen: () -> Unit,
    markAsViewed: suspend (ResultFilter) -> Unit,
    deleteResults: suspend (List<ResultModel.Id>) -> Unit = {},
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        state
            .map { it.filter }
            .distinctUntilChanged()
            .flatMapLatest {
                combine(
                    getResults(it),
                    getResultsStats(it),
                    ::Pair,
                )
            }.onEach { (results, stats) ->
                val previousRuns = _state.value.results.values
                    .flatten()
                val newRuns = previousRuns.updateWithNewResults(results)
                val newRunsByDate = newRuns.groupBy { it.run.startTime.date }
                _state.update { state ->
                    state.copy(
                        results = newRunsByDate,
                        stats = stats,
                        isLoading = false,
                        markAllAsViewedEnabled = results.any { !it.result.isViewed },
                    )
                }
            }.launchIn(viewModelScope)

        getDescriptors()
            .onEach { descriptors -> _state.update { it.copy(descriptors = descriptors) } }
            .launchIn(viewModelScope)

        getNetworks()
            .onEach { networks -> _state.update { it.copy(networks = networks) } }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.Start>()
            .onEach { markJustFinishedTestAsSeen() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ResultClick>()
            .onEach { goToResult(it.result.idOrThrow) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.UploadClick>()
            .onEach { goToUpload() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.MarkAsViewedClick>()
            .onEach { markAsViewed(state.value.filter) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DeleteClick>()
            .onEach {
                if (!state.value.selectionEnabled) {
                    deleteResultsByFilter(state.value.filter)
                } else {
                    val selectedIds = _state.value.selectedResultsIds
                    if (selectedIds.isNotEmpty()) {
                        deleteResults(selectedIds)
                        _state.update { state ->
                            state.copy(selectionEnabled = false)
                        }
                    }
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.FilterChanged>()
            .onEach { event -> _state.update { it.copy(filter = event.filter) } }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ChangeItemSelection>()
            .onEach { event ->
                _state.update { state ->
                    state.copy(
                        results = state.results.mapValues { (_, runs) ->
                            runs.map { it.changeSelection(event.item, event.isSelected) }
                        },
                        selectionEnabled = true,
                    )
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.CancelSelection>()
            .onEach { _ ->
                _state.update { state ->
                    state.copy(
                        results = state.results.mapValues { (_, runs) ->
                            runs.map { it.changeAllSelections(false) }
                        },
                        selectionEnabled = false,
                    )
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ToggleSelection>()
            .onEach {
                _state.update { state ->
                    val allSelected = state.areAllSelected
                    state.copy(
                        results = state.results.mapValues { (_, runs) ->
                            runs.map { it.changeAllSelections(!allSelected) }
                        },
                    )
                }
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val filter: ResultFilter = ResultFilter(),
        val descriptors: List<Descriptor> = emptyList(),
        val networks: List<NetworkModel> = emptyList(),
        val results: Map<LocalDate, List<RunListItem>> = emptyMap(),
        val stats: ResultsStats? = null,
        val isLoading: Boolean = true,
        val markAllAsViewedEnabled: Boolean = false,
        val selectionEnabled: Boolean = false,
    ) {
        private val allRuns get() = results.values.flatten()
        private val allResultItems get() = allRuns.flatMap { it.results }
        val areResultsLimited get() = allResultItems.size >= ResultFilter.LIMIT
        val anyMissingUpload get() = allResultItems.any { it.item.anyMeasurementUploadFailed }
        val areAllSelected get() = allResultItems.all { it.isSelected }
        val isAnySelected get() = allResultItems.any { it.isSelected }
        val selectedResultsCount get() = allResultItems.count { it.isSelected }
        val selectedResultsIds
            get() = allResultItems.filter { it.isSelected }.map { it.item.idOrThrow }
    }

    sealed interface Event {
        data object Start : Event

        data class ResultClick(
            val result: ResultListItem,
        ) : Event

        data object UploadClick : Event

        data object MarkAsViewedClick : Event

        data object DeleteClick : Event

        data class ChangeItemSelection(
            val item: ResultListItem,
            val isSelected: Boolean,
        ) : Event

        data object CancelSelection : Event

        data object ToggleSelection : Event

        data class FilterChanged(
            val filter: ResultFilter,
        ) : Event
    }
}
