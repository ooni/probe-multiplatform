package ui.screens.home

import cafe.adriel.voyager.core.model.ScreenModel
import core.settings.SettingsStore

class HomeScreenModel(
    private val settingsStore: SettingsStore,
) : ScreenModel {
    fun clearSettings() {
        settingsStore.clearAll()
    }
}