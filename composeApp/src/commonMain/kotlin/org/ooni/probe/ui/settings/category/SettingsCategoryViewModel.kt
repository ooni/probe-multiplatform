package org.ooni.probe.ui.settings.category

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.data.repositories.SettingsKey

class SettingsCategoryViewModel(
    preferenceManager: PreferenceRepository,
    goToSettingsForCategory: (String) -> Unit,
    onBack: () -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    init {
        events.filterIsInstance<Event.SettingsCategoryClick>()
            .onEach { goToSettingsForCategory(it.category) }.launchIn(viewModelScope)
        events.filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    val settings: StateFlow<Map<String, Any?>?> =
        preferenceManager
            .allSettings(listOf(booleanPreferencesKey(SettingsKey.NOTIFICATIONS_ENABLED.value)))
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000L),
                null,
            )

    sealed interface Event {
        data class SettingsCategoryClick(val category: String) : Event

        data class CheckedChangeClick(val key: String, val value: Boolean) : Event

        data object BackClicked : Event
    }
}
