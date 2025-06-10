package org.ooni.probe.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.ResultFilter
import org.ooni.probe.data.models.ResultListItem
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.ui.shared.SelectableItem

class ResultsViewModel(
    goToResult: (ResultModel.Id) -> Unit,
    goToUpload: () -> Unit,
    getResults: (ResultFilter) -> Flow<List<ResultListItem>>,
    getDescriptors: () -> Flow<List<Descriptor>>,
    deleteAllResults: suspend () -> Unit,
    markJustFinishedTestAsSeen: () -> Unit,
    markAllAsViewed: suspend () -> Unit,
    deleteResults: suspend (List<ResultModel.Id>) -> Unit = {},
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        state
            .map { it.filter }
            .distinctUntilChanged()
            .flatMapLatest { getResults(it) }
            .onEach { results ->
                val groupedResults = results.groupBy { it.monthAndYear }
                    .mapValues { entry ->
                        entry.value.map { item ->
                            // Use selectionEnabled to determine if selection is active, but default to false for all
                            SelectableItem(item, false)
                        }
                    }
                _state.update { state ->
                    state.copy(
                        results = groupedResults,
                        summary = results.toSummary(),
                        isLoading = false,
                        markAllAsViewedEnabled = results.any { !it.result.isViewed },
                    )
                }
            }
            .launchIn(viewModelScope)

        getDescriptors()
            .onEach { descriptors ->
                _state.update { state ->
                    state.copy(
                        descriptorFilters =
                            listOf(ResultFilter.Type.All) +
                                descriptors.map { ResultFilter.Type.One(it) },
                    )
                }
            }
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
            .filterIsInstance<Event.MarkAllAsViewedClick>()
            .onEach { markAllAsViewed() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DeleteClick>()
            .onEach {
                if (!state.value.selectionEnabled) {
                    deleteAllResults()
                } else {

                    val selectedIds = _state.value.results.values.flatMap { list -> // Suggestion 1.1
                        list.filter { it.isSelected }.map { it.item.idOrThrow }
                    }
                    if (selectedIds.isNotEmpty()) {
                        deleteResults(selectedIds)
                        // Deselect all after deletion
                        _state.update {
                            it.copy(
                                results = it.results.mapValues {
                                        (_, list) ->
                                    list.map { item -> item.copy(isSelected = false) }
                                },
                                selectionEnabled = false,
                            )
                        }
                    }
                }
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DescriptorFilterChanged>()
            .onEach { event ->
                _state.update { it.copy(filter = it.filter.copy(descriptor = event.filterType)) }
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.OriginFilterChanged>()
            .onEach { event ->
                _state.update { it.copy(filter = it.filter.copy(taskOrigin = event.filterType)) }
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ToggleItemSelection>()
            .onEach { event ->
                _state.update { state ->
                    state.copy(
                        results = state.results.mapValues { (_, list) ->
                            list.map { item ->
                                if (item.item.idOrThrow == event.item.idOrThrow) item.copy(isSelected = event.selected) else item
                            }
                        },
                        selectionEnabled = state.selectionEnabled || event.selected,
                    )
                }
            }
            .launchIn(viewModelScope)
        events
            .filterIsInstance<Event.CancelSelection>()
            .onEach { event ->
                _state.update { state ->
                    state.copy(
                        results = state.results.mapValues { (_, list) -> list.map { it.copy(isSelected = false) } },
                        selectionEnabled = false,
                    )
                }
            }
            .launchIn(viewModelScope)
        events
            .filterIsInstance<Event.ToggleSelection>()
            .onEach {
                val state = _state.value
                val allSelected = state.areAllSelected
                _state.update { s ->
                    s.copy(
                        results = s.results.mapValues { (_, list) ->
                            list.map { item -> item.copy(isSelected = !allSelected) }
                        },
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private val ResultListItem.monthAndYear
        get() = result.startTime.let { startTime ->
            LocalDate(year = startTime.year, month = startTime.month, dayOfMonth = 1)
        }

    data class State(
        val filter: ResultFilter = ResultFilter(),
        val descriptorFilters: List<ResultFilter.Type<Descriptor>> = listOf(
            ResultFilter.Type.All,
        ),
        val originFilters: List<ResultFilter.Type<TaskOrigin>> = listOf(
            ResultFilter.Type.All,
            ResultFilter.Type.One(TaskOrigin.OoniRun),
            ResultFilter.Type.One(TaskOrigin.AutoRun),
        ),
        val results: Map<LocalDate, List<SelectableItem<ResultListItem>>> = emptyMap(),
        val summary: Summary? = null,
        val isLoading: Boolean = true,
        val markAllAsViewedEnabled: Boolean = false,
        val selectionEnabled: Boolean = false,
    ) {
        val anyMissingUpload
            get() = results.any { it.value.any { item -> !item.item.allMeasurementsUploaded } }

        val areResultsLimited get() = results.values.sumOf { it.size } >= ResultFilter.LIMIT
        val areAllSelected get() = results.values.flatten().all { it.isSelected } && results.values.flatten().isNotEmpty()
        val isAnySelected get() = results.values.flatten().any { it.isSelected }
        val selectedResultsCount get() = results.values.flatten().count { it.isSelected }
    }

    data class Summary(
        val resultsCount: Int,
        val networksCount: Int,
        val dataUsageUp: Long,
        val dataUsageDown: Long,
    )

    private fun List<ResultListItem>.toSummary() =
        Summary(
            resultsCount = size,
            networksCount = mapNotNull { it.network }.distinct().size,
            dataUsageUp = sumOf { it.result.dataUsageUp },
            dataUsageDown = sumOf { it.result.dataUsageDown },
        )

    sealed interface Event {
        data object Start : Event

        data class ResultClick(val result: ResultListItem) : Event

        data object UploadClick : Event

        data object MarkAllAsViewedClick : Event

        data object DeleteClick : Event

        data class DescriptorFilterChanged(val filterType: ResultFilter.Type<Descriptor>) : Event

        data class OriginFilterChanged(val filterType: ResultFilter.Type<TaskOrigin>) : Event

        data class ToggleItemSelection(val item: ResultListItem, val selected: Boolean) : Event

        data object CancelSelection : Event

        data object ToggleSelection : Event
    }
}
