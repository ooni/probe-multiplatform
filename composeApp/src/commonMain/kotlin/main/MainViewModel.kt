package main

import cafe.adriel.voyager.core.model.ScreenModel
import core.settings.SettingsStore
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    settingsStore: SettingsStore,
) : ScreenModel {
    val onboardingState: StateFlow<OnboardingState> =
        settingsStore.getProbeCredentials().map {
            if (it.isNullOrEmpty().not()) {
                return@map OnboardingState.Complete
            }
            OnboardingState.Incomplete
        }.stateIn(
            scope = screenModelScope,
            started=SharingStarted.WhileSubscribed(),
            initialValue = OnboardingState.Loading,
        )
}

sealed class OnboardingState {
    data object Loading: OnboardingState()
    data object Complete : OnboardingState()
    data object Incomplete: OnboardingState()
}

