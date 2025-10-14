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
import org.ooni.probe.data.models.DescriptorType
import org.ooni.probe.data.models.DescriptorUpdateOperationState
import org.ooni.probe.data.models.DescriptorsUpdateState
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.SettingsKey

class DescriptorsViewModel(
    goToDescriptor: (String) -> Unit,
    goToReviewDescriptorUpdates: (List<InstalledTestDescriptorModel.Id>?) -> Unit,
    getTestDescriptors: () -> Flow<List<Descriptor>>,
    observeDescriptorUpdateState: () -> Flow<DescriptorsUpdateState>,
    startDescriptorsUpdates: suspend (List<InstalledTestDescriptorModel>?) -> Unit,
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
                _state.update {
                    it.copy(
                        availableUpdates = updates.availableUpdates.toList(),
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
            .onEach { event -> goToDescriptor(event.descriptor.key) }
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
                goToReviewDescriptorUpdates(
                    listOf(
                        (it.descriptor.source as? Descriptor.Source.Installed)?.value?.id
                            ?: return@onEach,
                    ),
                )
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.CancelUpdatesClicked>()
            .onEach { dismissDescriptorsUpdateNotice() }
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

    private fun List<Descriptor>.groupByType(collapsedSections: List<DescriptorType>) =
        listOf(
            DescriptorSection(
                type = DescriptorType.Installed,
                descriptors = filter { it.source is Descriptor.Source.Installed },
                isCollapsed = collapsedSections.contains(DescriptorType.Installed),
            ),
            DescriptorSection(
                type = DescriptorType.Default,
                descriptors = filter { it.source is Descriptor.Source.Default },
                isCollapsed = collapsedSections.contains(DescriptorType.Default),
            ),
        )

    private fun Any?.toCollapsedSections() =
        @Suppress("UNCHECKED_CAST")
        (this as? Set<String>)?.mapNotNull { DescriptorType.fromKey(it) }.orEmpty()

    data class State(
        val sections: List<DescriptorSection> = emptyList(),
        val availableUpdates: List<InstalledTestDescriptorModel> = emptyList(),
        val descriptorsUpdateOperationState: DescriptorUpdateOperationState = DescriptorUpdateOperationState.Idle,
        val canPullToRefresh: Boolean = true,
    ) {
        val isRefreshing: Boolean
            get() = descriptorsUpdateOperationState == DescriptorUpdateOperationState.FetchingUpdates

        val isRefreshEnabled: Boolean
            get() = sections
                .firstOrNull { it.type == DescriptorType.Installed }
                ?.descriptors
                ?.any() == true
    }

    sealed interface Event {
        data class DescriptorClicked(
            val descriptor: Descriptor,
        ) : Event

        data class ToggleSection(
            val type: DescriptorType,
        ) : Event

        data class UpdateDescriptorClicked(
            val descriptor: Descriptor,
        ) : Event

        data object FetchUpdatedDescriptors : Event

        data object ReviewUpdatesClicked : Event

        data object CancelUpdatesClicked : Event
    }

    data class DescriptorSection(
        val type: DescriptorType,
        val descriptors: List<Descriptor>,
        val isCollapsed: Boolean = false,
    )
}
