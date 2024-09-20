package org.ooni.probe.ui.descriptor

import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorUpdatesStatus
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.models.UpdateStatus
import org.ooni.probe.data.models.UpdateStatusType
import org.ooni.probe.data.models.toDescriptor
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.ui.shared.SelectableItem
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class DescriptorViewModel(
    private val descriptorKey: String,
    onBack: () -> Unit,
    private val getTestDescriptors: () -> Flow<List<Descriptor>>,
    getDescriptorLastResult: (String) -> Flow<ResultModel?>,
    private val preferenceRepository: PreferenceRepository,
    private val launchUrl: (String) -> Unit,
    deleteTestDescriptor: suspend (InstalledTestDescriptorModel) -> Unit,
    fetchDescriptorUpdate:
        suspend (
            List<InstalledTestDescriptorModel>,
        ) -> Unit,
    setAutoUpdate: suspend (InstalledTestDescriptorModel.Id, Boolean) -> Unit,
    reviewUpdates: (List<InstalledTestDescriptorModel>) -> Unit,
    descriptorUpdates: () -> Flow<DescriptorUpdatesStatus>,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {

        descriptorUpdates().onEach { results ->
            _state.update {
                it.copy(
                    refreshType = if (results.refreshType != UpdateStatusType.ReviewLink) results.refreshType else UpdateStatusType.None,
                )
            }
            if (results.availableUpdates.size == 1) {
                results.availableUpdates.first().let { updatedDescriptor ->
                    if (updatedDescriptor.id.value == descriptorKey.toLongOrNull()) {
                        _state.update {
                            it.copy(
                                updatedDescriptor = updatedDescriptor.toDescriptor(),
                                refreshType = UpdateStatusType.None,
                            )
                        }
                    }
                }
            }
        }.launchIn(viewModelScope)
        getDescriptor().onEach { if (it == null) onBack() }.filterNotNull()
            .flatMapLatest { descriptor ->

                combine(
                    preferenceRepository.areNetTestsEnabled(
                        list = descriptor.allTests.map { descriptor to it },
                        isAutoRun = true,
                    ),
                    getMaxRuntime(),
                ) { preferences, maxRuntime ->
                    _state.update {
                        it.copy(
                            descriptor = descriptor,
                            estimatedTime = descriptor.estimatedDuration.coerceAtMost(
                                maxRuntime ?: Duration.INFINITE,
                            ),
                            tests = descriptor.allTests.map { test ->
                                SelectableItem(
                                    item = test,
                                    isSelected = preferences[descriptor to test] == true,
                                )
                            },
                        )
                    }
                }
            }.launchIn(viewModelScope)

        getDescriptorLastResult(descriptorKey).onEach { lastResult ->
            _state.update {
                it.copy(lastResult = lastResult)
            }
        }.launchIn(viewModelScope)

        events.filterIsInstance<Event.BackClicked>().onEach { onBack() }.launchIn(viewModelScope)

        events.filterIsInstance<Event.AllChecked>().onEach {
            val descriptor = state.value.descriptor ?: return@onEach
            val allTestsSelected = state.value.tests.all { it.isSelected }
            preferenceRepository.setAreNetTestsEnabled(
                list = descriptor.allTests.map { descriptor to it },
                isAutoRun = true,
                isEnabled = !allTestsSelected,
            )
        }.launchIn(viewModelScope)

        events.filterIsInstance<Event.TestChecked>().onEach {
            val descriptor = state.value.descriptor ?: return@onEach
            preferenceRepository.setAreNetTestsEnabled(
                list = listOf(descriptor to it.test),
                isAutoRun = true,
                isEnabled = it.isChecked,
            )
        }.launchIn(viewModelScope)

        events.filterIsInstance<Event.UninstallClicked>().onEach {
            deleteTestDescriptor(it.value)
        }.launchIn(viewModelScope)

        events.filterIsInstance<Event.RevisionClicked>().onEach {
            launchUrl(
                "${OrganizationConfig.ooniRunDashboardUrl}/revisions/$descriptorKey?revision=${it.revision}",
            )
        }.launchIn(viewModelScope)

        events.filterIsInstance<Event.SeeMoreRevisionsClicked>().onEach {

            launchUrl(
                "${OrganizationConfig.ooniRunDashboardUrl}/revisions/$descriptorKey",
            )
        }.launchIn(viewModelScope)

        events.filterIsInstance<Event.AutoUpdateChanged>().onEach {

            val descriptor = state.value.descriptor ?: return@onEach
            if (descriptor.source !is Descriptor.Source.Installed) return@onEach
            setAutoUpdate(descriptor.source.value.id, it.value)
        }.launchIn(viewModelScope)

        events.filterIsInstance<Event.FetchUpdatedDescriptor>().onEach {

            Logger.d("onEach: FetchUpdatedDescriptor")
            if (state.value.isRefreshing) return@onEach
            val descriptor = state.value.descriptor ?: return@onEach

            if (descriptor.source !is Descriptor.Source.Installed || descriptor.updateStatus is UpdateStatus.UpdateRejected) return@onEach
            _state.update {
                it.copy(refreshType = UpdateStatusType.UpdateLink, updatedDescriptor = null)
            }

            fetchDescriptorUpdate(listOf(descriptor.source.value))
        }.launchIn(viewModelScope)

        events.filterIsInstance<Event.UpdateDescriptor>().onEach {
            val descriptor = state.value.updatedDescriptor ?: return@onEach
            if (descriptor.source !is Descriptor.Source.Installed) return@onEach
            reviewUpdates(listOf(descriptor.source.value))
            _state.update {
                it.copy(refreshType = UpdateStatusType.None, updatedDescriptor = null)
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private fun getDescriptor() = getTestDescriptors().map { list -> list.firstOrNull { it.key == descriptorKey } }

    private fun getMaxRuntime(): Flow<Duration?> =
        preferenceRepository.allSettings(
            listOf(
                SettingsKey.MAX_RUNTIME_ENABLED,
                SettingsKey.MAX_RUNTIME,
            ),
        ).map { preferences ->
            val enabled = preferences[SettingsKey.MAX_RUNTIME_ENABLED] == true
            val value = preferences[SettingsKey.MAX_RUNTIME] as? Int
            if (enabled && value != null) {
                value.seconds
            } else {
                null
            }
        }

    data class State(
        val descriptor: Descriptor? = null,
        val updatedDescriptor: Descriptor? = null,
        val estimatedTime: Duration? = null,
        val tests: List<SelectableItem<NetTest>> = emptyList(),
        val lastResult: ResultModel? = null,
        val refreshType: UpdateStatusType = UpdateStatusType.None,
    ) {
        val isRefreshing: Boolean
            get() = refreshType != UpdateStatusType.None
        val allState
            get() = when (tests.count { it.isSelected }) {
                0 -> ToggleableState.Off
                tests.size -> ToggleableState.On
                else -> ToggleableState.Indeterminate
            }
    }

    sealed interface Event {
        data object BackClicked : Event

        data object AllChecked : Event

        data class TestChecked(val test: NetTest, val isChecked: Boolean) : Event

        data class UninstallClicked(val value: InstalledTestDescriptorModel) : Event

        data class RevisionClicked(val revision: String) : Event

        data class AutoUpdateChanged(val value: Boolean) : Event

        data object SeeMoreRevisionsClicked : Event

        data object FetchUpdatedDescriptor : Event

        data object UpdateDescriptor : Event
    }
}
