package org.ooni.probe.ui.descriptors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorItem
import org.ooni.probe.data.models.DescriptorType
import org.ooni.probe.data.models.DescriptorUpdateOperationState
import org.ooni.probe.data.models.DescriptorsUpdateState
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.models.toDescriptorItem

class DescriptorsViewModel(
    goToDescriptor: (Descriptor.Id) -> Unit,
    goToReviewDescriptorUpdates: (List<Descriptor.Id>?) -> Unit,
    goToAddDescriptorUrl: () -> Unit,
    getTestDescriptors: () -> Flow<List<DescriptorItem>>,
    observeDescriptorUpdateState: () -> Flow<DescriptorsUpdateState>,
    startDescriptorsUpdates: suspend (List<Descriptor>?) -> Unit,
    dismissDescriptorsUpdateNotice: () -> Unit,
    canPullToRefresh: Boolean,
    private val getPreference: (SettingsKey) -> Flow<Any?>,
    private val setPreference: suspend (SettingsKey, Any?) -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State(canPullToRefresh = canPullToRefresh))
    val state = _state.asStateFlow()

    init {
        observeDescriptorUpdateState()
            .onEach { updates ->
                _state.update { state ->
                    state.copy(
                        availableUpdates = updates.availableUpdates.map { it.toDescriptorItem() },
                        descriptorsUpdateOperationState = updates.operationState,
                    )
                }
            }.launchIn(viewModelScope)

        combine(
            getTestDescriptors(),
            getPreference(SettingsKey.DESCRIPTOR_SECTIONS_COLLAPSED),
        ) { tests, collapsedSectionsPreference ->
            val collapsedSections = collapsedSectionsPreference.toCollapsedSections()
            _state.update { it.copy(sections = tests.groupByType(collapsedSections)) }
        }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DescriptorClicked>()
            .onEach { event -> goToDescriptor(event.descriptor.descriptor.id) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ToggleSection>()
            .onEach { (type) -> toggleSection(type) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.FetchUpdatedDescriptors>()
            .onEach { startDescriptorsUpdates(null) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ReviewUpdatesClicked>()
            .onEach {
                dismissDescriptorsUpdateNotice()
                goToReviewDescriptorUpdates(null)
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.UpdateDescriptorClicked>()
            .onEach {
                dismissDescriptorsUpdateNotice()
                goToReviewDescriptorUpdates(listOf(it.descriptor.descriptor.id))
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.CancelUpdatesClicked>()
            .onEach { dismissDescriptorsUpdateNotice() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.AddClicked>()
            .onEach { goToAddDescriptorUrl() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.FilterClicked>()
            .onEach { _state.update { it.copy(filterText = "") } }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.FilterTextChanged>()
            .onEach { event -> _state.update { it.copy(filterText = event.text) } }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.CloseFilterClicked>()
            .onEach { _state.update { it.copy(filterText = null) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private suspend fun toggleSection(type: DescriptorType) {
        val collapsedSections = getPreference(SettingsKey.DESCRIPTOR_SECTIONS_COLLAPSED)
            .first()
            .toCollapsedSections()
        val newCollapsedSections = if (collapsedSections.contains(type)) {
            collapsedSections - type
        } else {
            collapsedSections + type
        }
        setPreference(
            SettingsKey.DESCRIPTOR_SECTIONS_COLLAPSED,
            newCollapsedSections.map { it.key }.toSet(),
        )
    }

    private fun List<DescriptorItem>.groupByType(collapsedSections: List<DescriptorType>) =
        listOf(
            DescriptorSection(
                type = DescriptorType.Installed,
                descriptors = filter { !it.isDefault() },
                isCollapsed = collapsedSections.contains(DescriptorType.Installed),
            ),
            DescriptorSection(
                type = DescriptorType.Default,
                descriptors = filter { it.isDefault() },
                isCollapsed = collapsedSections.contains(DescriptorType.Default),
            ),
        )

    private fun Any?.toCollapsedSections() =
        @Suppress("UNCHECKED_CAST")
        (this as? Set<String>)?.mapNotNull { DescriptorType.fromKey(it) }.orEmpty()

    data class State(
        val sections: List<DescriptorSection> = emptyList(),
        val availableUpdates: List<DescriptorItem> = emptyList(),
        val descriptorsUpdateOperationState: DescriptorUpdateOperationState = DescriptorUpdateOperationState.Idle,
        val canPullToRefresh: Boolean = true,
        val filterText: String? = null,
    ) {
        val isRefreshing: Boolean
            get() = descriptorsUpdateOperationState == DescriptorUpdateOperationState.FetchingUpdates

        val isFiltering get() = filterText != null
    }

    sealed interface Event {
        data class DescriptorClicked(
            val descriptor: DescriptorItem,
        ) : Event

        data class ToggleSection(
            val type: DescriptorType,
        ) : Event

        data class UpdateDescriptorClicked(
            val descriptor: DescriptorItem,
        ) : Event

        data object FetchUpdatedDescriptors : Event

        data object ReviewUpdatesClicked : Event

        data object CancelUpdatesClicked : Event

        data object AddClicked : Event

        data object FilterClicked : Event

        data class FilterTextChanged(
            val text: String,
        ) : Event

        data object CloseFilterClicked : Event
    }

    data class DescriptorSection(
        val type: DescriptorType,
        val descriptors: List<DescriptorItem>,
        val isCollapsed: Boolean = false,
    )
}
