package ui.screens.onboarding

import cafe.adriel.voyager.core.model.ScreenModel

import core.settings.SettingsStore

class OnboardingViewModel(
    private val settingsStore: SettingsStore,
) : ScreenModel {
    fun onboardingComplete() {
        settingsStore.saveProbeCredentials("dummy value")
    }

}