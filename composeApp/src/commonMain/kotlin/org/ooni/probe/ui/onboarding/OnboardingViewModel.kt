package org.ooni.probe.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.shared.PlatformInfo
import org.ooni.probe.ui.dashboard.DashboardViewModel.Event

class OnboardingViewModel(
    private val goToDashboard: () -> Unit,
    private val goToSettings: () -> Unit,
    platformInfo: PlatformInfo,
    private val preferenceRepository: PreferenceRepository,
    private val launchUrl: (String) -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(
        State(
            stepList = if (platformInfo.needsToRequestNotificationsPermission) {
                Step.entries
            } else {
                Step.entries - Step.RequestNotificationPermission
            },
        ),
    )
    val state = _state.asStateFlow()

    init {
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
        )
            .onEach { event ->
                preferenceRepository.setValueByKey(
                    SettingsKey.AUTOMATED_TESTING_ENABLED,
                    event == Event.AutoTestYesClicked,
                )
                moveToNextStep()
            }
            .launchIn(viewModelScope)

        merge(
            events.filterIsInstance<Event.CrashReportingYesClicked>(),
            events.filterIsInstance<Event.CrashReportingNoClicked>(),
        )
            .onEach { event ->
                preferenceRepository.setValueByKey(
                    SettingsKey.SEND_CRASH,
                    event == Event.CrashReportingYesClicked,
                )
                moveToNextStep()
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.RequestNotificationsPermissionClicked>()
            .onEach {
                preferenceRepository.setValueByKey(SettingsKey.NOTIFICATIONS_ENABLED, true)
                moveToNextStep()
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ChangeDefaultsClicked>()
            .onEach {
                preferenceRepository.setValueByKey(SettingsKey.FIRST_RUN, false)
                goToSettings()
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private suspend fun moveToNextStep() {
        val state = _state.value
        if (state.stepIndex < state.stepList.size - 1) {
            _state.value = state.copy(stepIndex = state.stepIndex + 1)
        } else {
            preferenceRepository.setValueByKey(SettingsKey.FIRST_RUN, false)
            goToDashboard()
        }
    }

    data class State(
        val stepIndex: Int = 0,
        val stepList: List<Step>,
    )

    enum class Step {
        WhatIs,
        HeadsUp,
        AutomatedTesting,
        CrashReporting,
        RequestNotificationPermission,
        DefaultSettings,
    }

    sealed interface Event {
        data object NextClicked : Event

        data object HeadsUpLearnMoreClicked : Event

        data object AutoTestYesClicked : Event

        data object AutoTestNoClicked : Event

        data object CrashReportingYesClicked : Event

        data object CrashReportingNoClicked : Event

        data object RequestNotificationsPermissionClicked : Event

        data object ChangeDefaultsClicked : Event
    }

    companion object {
        private const val LEARN_MORE_URL = "https://ooni.org/about/risks/"
    }
}
