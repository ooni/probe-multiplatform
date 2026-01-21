package org.ooni.probe.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ooni.probe.config.BatteryOptimization
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.shared.PlatformInfo
import kotlin.text.compareTo

class OnboardingViewModel(
    private val goToDashboard: () -> Unit,
    private val goToSettings: () -> Unit,
    platformInfo: PlatformInfo,
    private val preferenceRepository: PreferenceRepository,
    private val launchUrl: (String) -> Unit,
    private val batteryOptimization: BatteryOptimization,
    supportsCrashReporting: Boolean,
    isCleanUpRequired: () -> Flow<Boolean>,
    cleanupLegacyDirectories: (suspend () -> Boolean)?,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow<State>(State.BuildingSteps)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val steps = listOfNotNull(
                Step.WhatIs,
                Step.HeadsUp,
                Step.AutomatedTesting,
                if (supportsCrashReporting) Step.CrashReporting else null,
                if (platformInfo.requestNotificationsPermission) {
                    Step.RequestNotificationPermission
                } else {
                    null
                },
                if (isCleanUpRequired().first()) Step.ClearDanglingResources else null,
                Step.DefaultSettings,
            )
            _state.value = State.start(steps)
        }

        events
            .filterIsInstance<Event.NextClicked>()
            .onEach { moveToNextStep() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.HeadsUpLearnMoreClicked>()
            .onEach { launchUrl(LEARN_MORE_URL) }
            .launchIn(viewModelScope)

        merge(
            events.filterIsInstance<Event.AutoTestYesClicked>(),
            events.filterIsInstance<Event.AutoTestNoClicked>(),
        ).onEach { event ->
            val enableAutoTest = event == Event.AutoTestYesClicked

            preferenceRepository.setValueByKey(
                SettingsKey.AUTOMATED_TESTING_ENABLED,
                enableAutoTest,
            )

            if (enableAutoTest &&
                batteryOptimization.isSupported &&
                !batteryOptimization.isIgnoring
            ) {
                requestIgnoreBatteryOptimization()
            } else {
                moveToNextStep()
            }
        }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BatteryOptimizationOkClicked>()
            .onEach { requestIgnoreBatteryOptimization() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BatteryOptimizationCancelClicked>()
            .onEach { moveToNextStep() }
            .launchIn(viewModelScope)

        merge(
            events.filterIsInstance<Event.CrashReportingYesClicked>(),
            events.filterIsInstance<Event.CrashReportingNoClicked>(),
        ).onEach { event ->
            preferenceRepository.setValueByKey(
                SettingsKey.SEND_CRASH,
                event == Event.CrashReportingYesClicked,
            )
            moveToNextStep()
        }.launchIn(viewModelScope)

        merge(
            events.filterIsInstance<Event.RequestNotificationsPermissionSkipped>(),
            events.filterIsInstance<Event.RequestNotificationsPermissionDone>(),
        ).onEach { moveToNextStep() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.CleanupClicked>()
            .onEach {
                cleanupLegacyDirectories?.invoke()

                moveToNextStep()
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.SkipCleanupClicked>()
            .onEach {
                moveToNextStep()
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ChangeDefaultsClicked>()
            .onEach {
                preferenceRepository.setValueByKey(SettingsKey.FIRST_RUN, false)
                goToSettings()
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private suspend fun moveToNextStep() {
        val state = _state.value as? State.Ready ?: return
        if (!state.isLastStep) {
            _state.value = state.nextStep()
        } else {
            preferenceRepository.setValueByKey(SettingsKey.FIRST_RUN, false)
            goToDashboard()
        }
    }

    private fun requestIgnoreBatteryOptimization() {
        val state = _state.value as? State.Ready ?: return
        batteryOptimization.requestIgnore { isIgnoring ->
            if (isIgnoring) {
                viewModelScope.launch { moveToNextStep() }
            } else {
                _state.update {
                    state.copy(showBatteryOptimizationDialog = true)
                }
            }
        }
    }

    sealed interface State {
        data object BuildingSteps : State

        data class Ready(
            val stepIndex: Int,
            private val steps: List<Step>,
            val showBatteryOptimizationDialog: Boolean = false,
        ) : State {
            val step get() = steps[stepIndex]
            val totalSteps get() = steps.size
            val isLastStep get() = stepIndex == totalSteps - 1

            fun nextStep() =
                copy(
                    stepIndex = stepIndex + 1,
                    showBatteryOptimizationDialog = false,
                )
        }

        companion object {
            fun start(steps: List<Step>) =
                Ready(
                    stepIndex = 0,
                    steps = steps,
                )
        }
    }

    enum class Step {
        WhatIs,
        HeadsUp,
        AutomatedTesting,
        CrashReporting,
        RequestNotificationPermission,
        ClearDanglingResources,
        DefaultSettings,
    }

    sealed interface Event {
        data object NextClicked : Event

        data object HeadsUpLearnMoreClicked : Event

        data object AutoTestYesClicked : Event

        data object AutoTestNoClicked : Event

        data object BatteryOptimizationOkClicked : Event

        data object BatteryOptimizationCancelClicked : Event

        data object CrashReportingYesClicked : Event

        data object CrashReportingNoClicked : Event

        data object RequestNotificationsPermissionDone : Event

        data object RequestNotificationsPermissionSkipped : Event

        data object SkipCleanupClicked : Event

        data object CleanupClicked : Event

        data object ChangeDefaultsClicked : Event
    }

    companion object {
        private const val LEARN_MORE_URL = "https://ooni.org/about/risks/"
    }
}
