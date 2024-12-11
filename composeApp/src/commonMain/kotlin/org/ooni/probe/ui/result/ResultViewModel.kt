package org.ooni.probe.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementWithUrl
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.ResultItem
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.models.RunBackgroundState

class ResultViewModel(
    resultId: ResultModel.Id,
    onBack: () -> Unit,
    goToMeasurement: (MeasurementModel.ReportId, String?) -> Unit,
    goToUpload: () -> Unit,
    goToDashboard: () -> Unit,
    getResult: (ResultModel.Id) -> Flow<ResultItem?>,
    getCurrentRunBackgroundState: Flow<RunBackgroundState>,
    markResultAsViewed: suspend (ResultModel.Id) -> Unit,
    startBackgroundRun: (RunSpecification) -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    private val expandedDescriptorsKeys = MutableStateFlow(emptyList<TestType>())

    private val _state = MutableStateFlow(State(result = null, groupedMeasurements = emptyList()))
    val state = _state.asStateFlow()

    init {
        combine(
            getResult(resultId),
            expandedDescriptorsKeys,
            ::Pair,
        )
            .onEach { (result, expandedDescriptorsKeys) ->
                var groupedMeasurements = listOf<Any>()
                result?.measurements?.let { measurements ->
                    groupedMeasurements = measurements.groupBy { it.measurement.test.name }.flatMap { (_, itemList) ->
                        when {
                            itemList.size == 1 -> listOf(itemList.first())
                            itemList.size > 1 && itemList.size == measurements.size -> itemList
                            else -> {
                                val key = itemList.first().measurement.test
                                listOf(
                                    MeasurementGroup(
                                        test = key,
                                        measurements = itemList,
                                        isExpanded = expandedDescriptorsKeys.contains(key),
                                    ),
                                )
                            }
                        }
                    }
                }
                _state.update {
                    it.copy(result = result, groupedMeasurements = groupedMeasurements)
                }
                if (result?.result?.isViewed == false) {
                    markResultAsViewed(resultId)
                }
            }
            .launchIn(viewModelScope)

        combine(
            state.map { it.result }.distinctUntilChanged(),
            getCurrentRunBackgroundState,
        ) { resultItem, testState ->
            _state.update {
                it.copy(
                    rerunEnabled = resultItem?.canBeRerun == true && testState is RunBackgroundState.Idle,
                )
            }
        }
            .launchIn(viewModelScope)

        _state
            .map { it.result }
            .filterNotNull()
            .take(1)
            .onEach {
                if (!it.result.isViewed) {
                    markResultAsViewed(resultId)
                }
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.MeasurementClicked>()
            .onEach { goToMeasurement(it.measurementReportId, it.measurementInput) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.UploadClicked>()
            .onEach { goToUpload() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.RerunClicked>()
            .onEach {
                startBackgroundRun(getRerunSpecification() ?: return@onEach)
                goToDashboard()
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.MeasurementGroupToggled>()
            .onEach { event ->
                expandedDescriptorsKeys.update { keys ->
                    val key = event.measurementGroup.test
                    if (keys.contains(key)) {
                        keys - key
                    } else {
                        keys + key
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private fun getRerunSpecification(): RunSpecification? {
        val item = _state.value.result ?: return null
        return RunSpecification.Full(
            tests = listOf(
                RunSpecification.Test(
                    source = RunSpecification.Test.Source.fromDescriptor(item.descriptor),
                    netTests = listOf(
                        NetTest(
                            test = TestType.WebConnectivity,
                            inputs = item.measurements.mapNotNull { it.url?.url },
                        ),
                    ),
                ),
            ),
            taskOrigin = TaskOrigin.OoniRun,
            isRerun = true,
        )
    }

    data class State(
        val result: ResultItem?,
        val groupedMeasurements: List<Any>,
        val rerunEnabled: Boolean = false,
    )

    sealed interface Event {
        data object BackClicked : Event

        data class MeasurementClicked(
            val measurementReportId: MeasurementModel.ReportId,
            val measurementInput: String?,
        ) : Event

        data object UploadClicked : Event

        data object RerunClicked : Event

        data class MeasurementGroupToggled(val measurementGroup: MeasurementGroup) : Event
    }

    data class MeasurementGroup(
        val test: TestType,
        val measurements: List<MeasurementWithUrl>,
        val isExpanded: Boolean = false,
    )
}
