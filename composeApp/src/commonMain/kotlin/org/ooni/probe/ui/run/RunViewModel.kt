package org.ooni.probe.ui.run

import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorType
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.PlatformAction
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.models.notExpired
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.ui.shared.ParentSelectableItem
import org.ooni.probe.ui.shared.SelectableItem

class RunViewModel(
    onBack: () -> Unit,
    getTestDescriptors: () -> Flow<List<Descriptor>>,
    shouldShowVpnWarning: suspend () -> Boolean,
    private val preferenceRepository: PreferenceRepository,
    startBackgroundRun: (RunSpecification) -> Unit,
    openVpnSettings: (PlatformAction.VpnSettings) -> Boolean,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State(emptyMap()))
    val state = _state.asStateFlow()

    private val allNetTests = MutableStateFlow(emptyList<Pair<Descriptor, NetTest>>())
    private val expandedDescriptorsKeys = MutableStateFlow(emptyList<String>())
    private val selectedTests = MutableStateFlow<List<Pair<Descriptor, NetTest>>?>(null)

    init {
        // Initially selected tests based on the descriptor provided or the preferences
        viewModelScope.launch {
            val all = getTestDescriptors().first().notExpired().toNetTestsList()
            allNetTests.value = all

            val preferences = preferenceRepository
                .areNetTestsEnabled(all, isAutoRun = false)
                .first()
            selectedTests.value = all.filter { preferences[it] == true }
        }

        combine(
            allNetTests,
            expandedDescriptorsKeys,
            selectedTests.filterNotNull(),
            ::Triple,
        ).map { (all, expandedKeys, selectedTests) ->
            val descriptorsWithTests = all
                .groupBy(keySelector = { it.first }, valueTransform = { it.second })
                .map { (descriptor, tests) ->
                    val selectedTestsCount = selectedTests.count { it.first == descriptor }
                    val containsExpandedKey = expandedKeys.contains(descriptor.key)

                    ParentSelectableItem(
                        item = descriptor,
                        state = when {
                            !descriptor.enabled || selectedTestsCount == 0 -> ToggleableState.Off
                            selectedTestsCount == tests.size -> ToggleableState.On
                            else -> ToggleableState.Indeterminate
                        },
                        isExpanded = containsExpandedKey,
                    ) to tests.map { test ->
                        SelectableItem(
                            item = test,
                            isSelected = descriptor.enabled &&
                                selectedTests.contains(descriptor to test),
                        )
                    }
                }.toMap()

            mapOf(
                DescriptorType.Installed to descriptorsWithTests
                    .filter { it.key.item.source is Descriptor.Source.Installed },
                DescriptorType.Default to descriptorsWithTests
                    .filter { it.key.item.source is Descriptor.Source.Default },
            )
        }.onEach { list -> _state.update { it.copy(list = list) } }
            .launchIn(viewModelScope)

        merge(
            events.filterIsInstance<Event.Start>(),
            events.filterIsInstance<Event.DisableVpnInstructionsDismissed>(),
        ).onEach { _state.update { it.copy(showVpnWarning = shouldShowVpnWarning()) } }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.SelectAllClicked>()
            .onEach {
                selectedTests.value = allNetTests.value
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DeselectAllClicked>()
            .onEach { selectedTests.value = emptyList() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DescriptorChecked>()
            .onEach { event ->
                selectedTests.update {
                    if (event.isChecked) {
                        (it.orEmpty() + event.descriptor.toNetTestsPairs()).distinct()
                    } else {
                        it.orEmpty() - event.descriptor.toNetTestsPairs().toSet()
                    }
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.NetTestChecked>()
            .onEach { event ->
                selectedTests.update {
                    if (event.isChecked) {
                        (it.orEmpty() + (event.descriptor to event.netTest)).distinct()
                    } else {
                        it.orEmpty() - (event.descriptor to event.netTest)
                    }
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DescriptorDropdownToggled>()
            .onEach { event ->
                expandedDescriptorsKeys.update { keys ->
                    val key = event.descriptor.key
                    if (keys.contains(key)) {
                        keys - key
                    } else {
                        keys + key
                    }
                }
            }.launchIn(viewModelScope)

        merge(
            events.filterIsInstance<Event.RunClicked>(),
            events.filterIsInstance<Event.RunAlwaysClicked>(),
        ).onEach {
            if (it is Event.RunAlwaysClicked) {
                preferenceRepository.setValueByKey(SettingsKey.WARN_VPN_IN_USE, false)
            }
            saveRunPreferences()
            startBackgroundRun(buildRunSpecification())
            onBack()
        }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DisableVpnClicked>()
            .onEach {
                if (!openVpnSettings(PlatformAction.VpnSettings)) {
                    _state.update { it.copy(showDisableVpnInstructions = true) }
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DisableVpnInstructionsDismissed>()
            .onEach { _state.update { it.copy(showDisableVpnInstructions = false) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private suspend fun saveRunPreferences() {
        val all = _state.value.list.toNetTestsList()
        val selected = selectedTests.value.orEmpty()
        setAreEnabled(all - selected.toSet(), false)
        setAreEnabled(selected.toList(), true)
    }

    private suspend fun setAreEnabled(
        list: List<Pair<Descriptor, NetTest>>,
        isEnabled: Boolean,
    ) {
        preferenceRepository.setAreNetTestsEnabled(
            list = list,
            isAutoRun = false,
            isEnabled = isEnabled,
        )
    }

    private fun List<Descriptor>.toNetTestsList(): List<Pair<Descriptor, NetTest>> = flatMap { it.toNetTestsPairs() }

    private fun Descriptor.toNetTestsPairs(): List<Pair<Descriptor, NetTest>> = allTests.map { this to it }

    private fun Map<DescriptorType, Map<ParentSelectableItem<Descriptor>, List<SelectableItem<NetTest>>>>.toNetTestsList() =
        flatMap { (_, map) ->
            map.flatMap { entry -> entry.value.map { entry.key.item to it.item } }
        }

    private fun buildRunSpecification(): RunSpecification =
        RunSpecification.Full(
            tests = selectedTests.value
                .orEmpty()
                .groupBy(keySelector = { it.first }, valueTransform = { it.second })
                .map { (descriptor, tests) ->
                    RunSpecification.Test(
                        source =
                            when (descriptor.source) {
                                is Descriptor.Source.Default ->
                                    RunSpecification.Test.Source.Default(descriptor.name)

                                is Descriptor.Source.Installed ->
                                    RunSpecification.Test.Source.Installed(descriptor.source.value.id)
                            },
                        netTests = tests,
                    )
                },
            taskOrigin = TaskOrigin.OoniRun,
            isRerun = false,
        )

    data class State(
        val list: Map<DescriptorType, Map<ParentSelectableItem<Descriptor>, List<SelectableItem<NetTest>>>>,
        val showVpnWarning: Boolean = false,
        val showDisableVpnInstructions: Boolean = false,
    )

    sealed interface Event {
        data object Start : Event

        data object BackClicked : Event

        data object SelectAllClicked : Event

        data object DeselectAllClicked : Event

        data class DescriptorChecked(
            val descriptor: Descriptor,
            val isChecked: Boolean,
        ) : Event

        data class DescriptorDropdownToggled(
            val descriptor: Descriptor,
        ) : Event

        data class NetTestChecked(
            val descriptor: Descriptor,
            val netTest: NetTest,
            val isChecked: Boolean,
        ) : Event

        data object RunClicked : Event

        data object RunAlwaysClicked : Event

        data object DisableVpnClicked : Event

        data object DisableVpnInstructionsDismissed : Event
    }
}
