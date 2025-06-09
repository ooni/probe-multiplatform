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
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.ResultFilter
import org.ooni.probe.data.models.ResultListItem
import org.ooni.probe.data.models.ResultModel

class ResultsViewModel(
    goToResult: (ResultModel.Id) -> Unit,
    goToUpload: () -> Unit,
    getResults: (ResultFilter) -> Flow<List<ResultListItem>>,
    getDescriptors: () -> Flow<List<Descriptor>>,
    deleteResultsByFilter: suspend (ResultFilter) -> Unit,
    markJustFinishedTestAsSeen: () -> Unit,
    markAsViewed: suspend (ResultFilter) -> Unit,
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
                    state.copy(descriptors = descriptors)
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
            .filterIsInstance<Event.MarkAsViewedClick>()
            .onEach { markAsViewed(state.value.filter) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DeleteClick>()
            .onEach { deleteResultsByFilter(state.value.filter) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.FilterChanged>()
            .onEach { event -> _state.update { it.copy(filter = event.filter) } }
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
        val descriptors: List<Descriptor> = emptyList(),
        val results: Map<LocalDate, List<ResultListItem>> = emptyMap(),
        val summary: Summary? = null,
        val isLoading: Boolean = true,
        val markAllAsViewedEnabled: Boolean = false,
    ) {
        val anyMissingUpload
            get() = results.any { it.value.any { item -> !item.allMeasurementsUploaded } }

        val areResultsLimited get() = results.values.sumOf { it.size } >= ResultFilter.LIMIT
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

        data object MarkAsViewedClick : Event

        data object DeleteClick : Event

        data class FilterChanged(val filter: ResultFilter) : Event
    }
}
