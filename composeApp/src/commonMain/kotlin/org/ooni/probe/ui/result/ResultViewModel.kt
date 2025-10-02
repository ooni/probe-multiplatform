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
import org.ooni.probe.data.models.RunBackgroundState
import org.ooni.probe.data.models.RunSpecification

class ResultViewModel(
    resultId: ResultModel.Id,
    onBack: () -> Unit,
    goToMeasurement: (MeasurementModel.Id) -> Unit,
    goToMeasurementRaw: (MeasurementModel.Id) -> Unit,
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
        ) { result, expandedDescriptorsKeys ->
            _state.update {
                it.copy(
                    result = result,
                    groupedMeasurements = groupMeasurements(result, expandedDescriptorsKeys),
                )
            }
            if (result?.result?.isViewed == false) {
                markResultAsViewed(resultId)
            }
        }.launchIn(viewModelScope)

        combine(
            state.map { it.result }.distinctUntilChanged(),
            getCurrentRunBackgroundState,
        ) { resultItem, testState ->
            _state.update {
                it.copy(
                    rerunEnabled = resultItem?.canBeRerun == true && testState is RunBackgroundState.Idle,
                )
            }
        }.launchIn(viewModelScope)

        _state
            .map { it.result }
            .filterNotNull()
            .take(1)
            .onEach {
                if (!it.result.isViewed) {
                    markResultAsViewed(resultId)
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.MeasurementClicked>()
            .onEach { event ->
                val measurement = event.item.measurement
                measurement.id?.let { measurementId ->
                    if (measurement.isMissingUpload) {
                        goToMeasurementRaw(measurementId)
                    } else {
                        goToMeasurement(measurementId)
                    }
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.UploadClicked>()
            .onEach { goToUpload() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.RerunClicked>()
            .onEach {
                startBackgroundRun(getRerunSpecification() ?: return@onEach)
                goToDashboard()
            }.launchIn(viewModelScope)

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
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private fun groupMeasurements(
        result: ResultItem?,
        expandedDescriptorsKeys: List<TestType>,
    ) = result
        ?.measurements
        ?.let { measurements ->
            measurements
                .groupBy { it.measurement.test }
                .flatMap { (key, itemList) ->
                    if (itemList.size == 1 || itemList.size == measurements.size) {
                        itemList.sort().map { MeasurementGroupItem.Single(it) }
                    } else {
                        listOf(
                            MeasurementGroupItem.Group(
                                test = key,
                                measurements = itemList.sort(),
                                isExpanded = expandedDescriptorsKeys.contains(key),
                            ),
                        )
                    }
                }
        }.orEmpty()

    private fun List<MeasurementWithUrl>.sort() =
        sortedWith(
            compareByDescending<MeasurementWithUrl> { it.measurement.isFailed }
                .thenByDescending { it.measurement.isAnomaly }
                .thenBy { it.measurement.startTime },
        )

    private fun getRerunSpecification(): RunSpecification? {
        val item = _state.value.result ?: return null
        return RunSpecification.Full(
            tests = listOf(
                RunSpecification.Test(
                    source = item.descriptor.source.id,
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
        val groupedMeasurements: List<MeasurementGroupItem>,
        val rerunEnabled: Boolean = false,
    )

    sealed interface Event {
        data object BackClicked : Event

        data class MeasurementClicked(
            val item: MeasurementWithUrl,
        ) : Event

        data object UploadClicked : Event

        data object RerunClicked : Event

        data class MeasurementGroupToggled(
            val measurementGroup: MeasurementGroupItem.Group,
        ) : Event
    }

    sealed class MeasurementGroupItem {
        data class Single(
            val measurement: MeasurementWithUrl,
        ) : MeasurementGroupItem()

        data class Group(
            val test: TestType,
            val measurements: List<MeasurementWithUrl>,
            val isExpanded: Boolean,
        ) : MeasurementGroupItem()
    }
}
