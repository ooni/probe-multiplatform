package org.ooni.probe.ui.descriptor

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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TestType
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorItem
import org.ooni.probe.data.models.DescriptorUpdateOperationState
import org.ooni.probe.data.models.DescriptorsUpdateState
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.PlatformAction
import org.ooni.probe.data.models.ResultListItem
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.shared.monitoring.Instrumentation
import org.ooni.probe.shared.monitoring.reportTransaction
import org.ooni.probe.ui.shared.SelectableItem
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class DescriptorViewModel(
    private val descriptorKey: String,
    private val onBack: () -> Unit,
    goToReviewDescriptorUpdates: (List<Descriptor.Id>?) -> Unit,
    goToChooseWebsites: () -> Unit,
    goToResult: (ResultModel.Id) -> Unit,
    goToDescriptorWebsites: (Descriptor.Id) -> Unit,
    getTestDescriptor: (String) -> Flow<DescriptorItem?>,
    getLastResultOfDescriptor: (String) -> Flow<ResultListItem?>,
    private val preferenceRepository: PreferenceRepository,
    private val launchAction: (PlatformAction) -> Boolean,
    shouldShowVpnWarning: suspend () -> Boolean,
    deleteTestDescriptor: suspend (Descriptor) -> Unit,
    startDescriptorsUpdate: suspend (List<Descriptor>?) -> Unit,
    setAutoUpdate: suspend (Descriptor.Id, Boolean) -> Unit,
    observeDescriptorsUpdateState: () -> Flow<DescriptorsUpdateState>,
    dismissDescriptorReviewNotice: () -> Unit,
    undoRejectedDescriptorUpdate: suspend (Descriptor.Id) -> Unit,
    private val startBackgroundRun: (RunSpecification) -> Unit,
    canPullToRefresh: Boolean,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State(canPullToRefresh = canPullToRefresh))
    val state = _state.asStateFlow()

    init {
        observeDescriptorsUpdateState()
            .onEach { updateState ->
                _state.update {
                    it.copy(
                        updateOperationState = updateState.operationState,
                    )
                }
            }.launchIn(viewModelScope)

        getTestDescriptor(descriptorKey)
            .onEach { if (it == null) onBack() }
            .filterNotNull()
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

        preferenceRepository
            .getValueByKey(SettingsKey.AUTOMATED_TESTING_ENABLED)
            .onEach { enabled ->
                _state.update {
                    it.copy(isAutoRunEnabled = enabled == true)
                }
            }.launchIn(viewModelScope)

        getLastResultOfDescriptor(descriptorKey)
            .onEach { lastResult ->
                _state.update {
                    it.copy(lastResult = lastResult)
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.AllChecked>()
            .onEach {
                val descriptor = state.value.descriptor ?: return@onEach
                val allTestsSelected = state.value.tests.all { it.isSelected }
                preferenceRepository.setAreNetTestsEnabled(
                    list = descriptor.allTests.map { descriptor to it },
                    isAutoRun = true,
                    isEnabled = !allTestsSelected,
                )
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.TestChecked>()
            .onEach {
                val descriptor = state.value.descriptor ?: return@onEach
                preferenceRepository.setAreNetTestsEnabled(
                    list = listOf(descriptor to it.test),
                    isAutoRun = true,
                    isEnabled = it.isChecked,
                )
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.UninstallClicked>()
            .onEach {
                deleteTestDescriptor(it.value)
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.RevisionClicked>()
            .onEach {
                launchAction(
                    PlatformAction.OpenUrl(
                        "${OrganizationConfig.ooniRunDashboardUrl}/revisions/$descriptorKey?revision=${it.revision}",
                    ),
                )
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.SeeMoreRevisionsClicked>()
            .onEach {
                launchAction(
                    PlatformAction.OpenUrl(
                        "${OrganizationConfig.ooniRunDashboardUrl}/revisions/$descriptorKey",
                    ),
                )
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.AutoUpdateChanged>()
            .onEach {
                val descriptor = state.value.descriptor ?: return@onEach
                setAutoUpdate(descriptor.source.id, it.value)
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.FetchUpdatedDescriptor>()
            .onEach {
                if (state.value.isRefreshing) return@onEach
                val descriptor = state.value.descriptor ?: return@onEach

                startDescriptorsUpdate(listOf(descriptor.source))
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.UpdateDescriptor>()
            .onEach {
                val newDescriptor = state.value.descriptor?.updatedDescriptor ?: return@onEach
                dismissDescriptorReviewNotice()
                goToReviewDescriptorUpdates(listOf(newDescriptor.id))
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.UndoRejectedRevisionClicked>()
            .onEach {
                val descriptor =
                    state.value.descriptor?.source
                        ?: return@onEach
                undoRejectedDescriptorUpdate(descriptor.id)
                startDescriptorsUpdate(listOf(descriptor))
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.RunClicked>()
            .onEach {
                if (shouldShowVpnWarning()) {
                    _state.update { it.copy(showVpnWarning = true) }
                } else {
                    startTest()
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.RunAnywaysClicked>()
            .onEach { startTest() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.RunAlwaysClicked>()
            .onEach {
                preferenceRepository.setValueByKey(SettingsKey.WARN_VPN_IN_USE, false)
                startTest()
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DisableVpnClicked>()
            .onEach {
                if (!launchAction(PlatformAction.VpnSettings)) {
                    _state.update {
                        it.copy(showVpnWarning = false, showDisableVpnInstructions = true)
                    }
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.VpnWarningDismissed>()
            .onEach { _state.update { it.copy(showVpnWarning = false) } }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DisableVpnInstructionsDismissed>()
            .onEach {
                _state.update {
                    it.copy(showVpnWarning = false, showDisableVpnInstructions = false)
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ChooseWebsitesClicked>()
            .onEach { goToChooseWebsites() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.EnableAutoRunClicked>()
            .onEach {
                preferenceRepository.setValueByKey(SettingsKey.AUTOMATED_TESTING_ENABLED, true)
                Instrumentation.reportTransaction("DescriptorEnableAutoRun")
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ResultClicked>()
            .onEach { event ->
                event.resultListItem.result.id
                    ?.let { goToResult(it) }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.SeeMoreWebsitesClicked>()
            .onEach {
                state.value.descriptor
                    ?.source
                    ?.let { installed -> goToDescriptorWebsites(installed.id) }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ShareClicked>()
            .onEach {
                val descriptor = state.value.descriptor ?: return@onEach
                val runLink = descriptor.runLink
                launchAction(PlatformAction.Share("${descriptor.name} $runLink"))
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private fun getMaxRuntime(): Flow<Duration?> =
        preferenceRepository
            .allSettings(
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

    private fun startTest() {
        val descriptor = state.value.descriptor ?: return
        startBackgroundRun(RunSpecification.buildForDescriptor(descriptor))
        onBack()
    }

    private fun buildRunSpecification(): RunSpecification? =
        state.value.descriptor?.let { descriptor ->
            RunSpecification.Full(
                tests = listOf(
                    RunSpecification.Test(
                        source = descriptor.source.id,
                        netTests = descriptor.allTests,
                    ),
                ),
                taskOrigin = TaskOrigin.OoniRun,
                isRerun = false,
            )
        }

    data class State(
        val descriptor: DescriptorItem? = null,
        val estimatedTime: Duration? = null,
        val tests: List<SelectableItem<NetTest>> = emptyList(),
        val lastResult: ResultListItem? = null,
        val updateOperationState: DescriptorUpdateOperationState = DescriptorUpdateOperationState.Idle,
        val isAutoRunEnabled: Boolean = true,
        val canPullToRefresh: Boolean = true,
        val showVpnWarning: Boolean = false,
        val showDisableVpnInstructions: Boolean = false,
    ) {
        val isRefreshing: Boolean
            get() = updateOperationState == DescriptorUpdateOperationState.FetchingUpdates
        val allState
            get() = when (tests.count { it.isSelected }) {
                0 -> ToggleableState.Off
                tests.size -> ToggleableState.On
                else -> ToggleableState.Indeterminate
            }
        val isRefreshEnabled get() = descriptor?.source != null
    }

    sealed interface Event {
        data object BackClicked : Event

        data object AllChecked : Event

        data class TestChecked(
            val test: NetTest,
            val isChecked: Boolean,
        ) : Event

        data class UninstallClicked(
            val value: Descriptor,
        ) : Event

        data class RevisionClicked(
            val revision: Long,
        ) : Event

        data object UndoRejectedRevisionClicked : Event

        data class AutoUpdateChanged(
            val value: Boolean,
        ) : Event

        data object SeeMoreRevisionsClicked : Event

        data object FetchUpdatedDescriptor : Event

        data object UpdateDescriptor : Event

        data object RunClicked : Event

        data object RunAnywaysClicked : Event

        data object RunAlwaysClicked : Event

        data object VpnWarningDismissed : Event

        data object DisableVpnClicked : Event

        data object DisableVpnInstructionsDismissed : Event

        data object ChooseWebsitesClicked : Event

        data object EnableAutoRunClicked : Event

        data class ResultClicked(
            val resultListItem: ResultListItem,
        ) : Event

        data object SeeMoreWebsitesClicked : Event

        data object ShareClicked : Event
    }
}

fun List<SelectableItem<NetTest>>.isSingleWebConnectivityTest(): Boolean =
    size == 1 && firstOrNull()?.item?.test == TestType.WebConnectivity
