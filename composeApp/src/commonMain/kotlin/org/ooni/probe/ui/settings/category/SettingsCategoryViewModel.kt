package org.ooni.probe.ui.settings.category

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.ooni.probe.data.repositories.PreferenceCategoryKey
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.ui.settings.SettingsCategoryItem

class SettingsCategoryViewModel(
    preferenceManager: PreferenceRepository,
    goToSettingsForCategory: (PreferenceCategoryKey) -> Unit,
    onBack: () -> Unit,
    category: SettingsCategoryItem,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State(preference = null, category = category))
    val state = _state.asStateFlow()

    init {
        category.settings?.map { item -> booleanPreferencesKey(item.key) }?.let { preferenceKeys ->
            preferenceManager.allSettings(preferenceKeys)
                .onEach { result -> _state.update { it.copy(preference = result) } }
                .launchIn(viewModelScope)
        }

        events.filterIsInstance<Event.SettingsCategoryClick>()
            .onEach { goToSettingsForCategory(it.category) }.launchIn(viewModelScope)

        events.filterIsInstance<Event.CheckedChangeClick>().onEach {
            preferenceManager.setValueByKey(
                booleanPreferencesKey(it.key),
                it.value,
            )
            _state.update { state ->
                state.copy(
                    preference = state.preference?.plus(it.key to it.value),
                )
            }
        }.launchIn(viewModelScope)

        events.filterIsInstance<Event.BackClicked>().onEach { onBack() }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val preference: Map<String, Any?>?,
        val category: SettingsCategoryItem,
    )

    sealed interface Event {
        data class SettingsCategoryClick(val category: PreferenceCategoryKey) : Event

        data class CheckedChangeClick(val key: String, val value: Boolean) : Event

        data object BackClicked : Event
    }
}
