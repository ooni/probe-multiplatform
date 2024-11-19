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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.config.TestDisplayMode
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorType
import org.ooni.probe.data.models.PlatformAction
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.models.SettingsKey
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

    private val collapsedDescriptorsKeys = MutableStateFlow(emptyList<String>())

    init {
        combine(
            getTestDescriptors(),
            collapsedDescriptorsKeys,
            ::Pair,
        ).flatMapLatest { (descriptors, collapsedDescriptorsKeys) ->
            preferenceRepository
                .areNetTestsEnabled(descriptors.toNetTestsList(), isAutoRun = false)
                .map { preferences ->
                    val descriptorsWithTests =
                        descriptors
                            .associate { descriptor ->
                                val tests = descriptor.allTests
                                val selectedTestsCount =
                                    tests.count { preferences[descriptor to it] == true }
                                val containsKey = collapsedDescriptorsKeys.contains(descriptor.key)

                                ParentSelectableItem(
                                    item = descriptor,
                                    state = when (selectedTestsCount) {
                                        0 -> ToggleableState.Off
                                        tests.size -> ToggleableState.On
                                        else -> ToggleableState.Indeterminate
                                    },
                                    isExpanded = when (OrganizationConfig.testDisplayMode) {
                                        TestDisplayMode.Regular -> !containsKey
                                        // Start with all descriptors collapsed
                                        TestDisplayMode.WebsitesOnly -> containsKey
                                    },
                                ) to tests.map { test ->
                                    SelectableItem(
                                        item = test,
                                        isSelected = preferences[descriptor to test] == true,
                                    )
                                }
                            }
                    mapOf(
                        DescriptorType.Default to descriptorsWithTests
                            .filter { it.key.item.source is Descriptor.Source.Default },
                        DescriptorType.Installed to descriptorsWithTests
                            .filter { it.key.item.source is Descriptor.Source.Installed },
                    )
                }
        }
            .onEach { list -> _state.update { it.copy(list = list) } }
            .launchIn(viewModelScope)

        merge(
            events.filterIsInstance<Event.Start>(),
            events.filterIsInstance<Event.DisableVpnInstructionsDismissed>(),
        )
            .onEach { _state.update { it.copy(showVpnWarning = shouldShowVpnWarning()) } }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.SelectAllClicked>()
            .onEach { setAllAreEnabled(true) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DeselectAllClicked>()
            .onEach { setAllAreEnabled(false) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DescriptorChecked>()
            .onEach {
                setAreEnabled(
                    it.descriptor.allTests.map { test -> it.descriptor to test },
                    isEnabled = it.isChecked,
                )
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.NetTestChecked>()
            .onEach { setIsEnabled(it.descriptor, it.netTest, isEnabled = it.isChecked) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DescriptorDropdownToggled>()
            .onEach { event ->
                collapsedDescriptorsKeys.update { keys ->
                    val key = event.descriptor.key
                    if (keys.contains(key)) {
                        keys - key
                    } else {
                        keys + key
                    }
                }
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.RunClicked>()
            .onEach {
                startBackgroundRun(buildRunSpecification())
                onBack()
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.RunAlwaysClicked>()
            .onEach {
                preferenceRepository.setValueByKey(SettingsKey.WARN_VPN_IN_USE, false)
                startBackgroundRun(buildRunSpecification())
                onBack()
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DisableVpnClicked>()
            .onEach {
                if (!openVpnSettings(PlatformAction.VpnSettings)) {
                    _state.update { it.copy(showDisableVpnInstructions = true) }
                }
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DisableVpnInstructionsDismissed>()
            .onEach { _state.update { it.copy(showDisableVpnInstructions = false) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private suspend fun setAllAreEnabled(isEnabled: Boolean) {
        setAreEnabled(
            _state.value.list.values
                .flatMap { it.entries }
                .flatMap { (descriptorItem, netTestItems) ->
                    netTestItems.map { netTestItem ->
                        descriptorItem.item to netTestItem.item
                    }
                },
            isEnabled = isEnabled,
        )
    }

    private suspend fun setIsEnabled(
        descriptor: Descriptor,
        test: NetTest,
        isEnabled: Boolean,
    ) {
        setAreEnabled(listOf(descriptor to test), isEnabled)
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

    private fun List<Descriptor>.toNetTestsList() =
        flatMap { descriptor ->
            descriptor.allTests.map { netTest ->
                descriptor to netTest
            }
        }

    private fun buildRunSpecification(): RunSpecification {
        val selectedTests = state.value.list.values
            .flatMap { it.entries }
            .associate { (descriptorItem, netTestItems) ->
                descriptorItem.item to netTestItems
                    .filter { it.isSelected }
                    .map { it.item }
            }
        return RunSpecification(
            tests =
                selectedTests.map { (descriptor, tests) ->
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
    }

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

        data class DescriptorChecked(val descriptor: Descriptor, val isChecked: Boolean) : Event

        data class DescriptorDropdownToggled(val descriptor: Descriptor) : Event

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
