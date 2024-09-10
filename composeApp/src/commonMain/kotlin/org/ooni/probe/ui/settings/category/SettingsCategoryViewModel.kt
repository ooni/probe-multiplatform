package org.ooni.probe.ui.settings.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.ooni.probe.data.models.PreferenceCategoryKey
import org.ooni.probe.data.models.SettingsCategoryItem
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository

class SettingsCategoryViewModel(
    categoryKey: String,
    goToSettingsForCategory: (PreferenceCategoryKey) -> Unit,
    onBack: () -> Unit,
    getSettings: () -> Flow<List<SettingsCategoryItem>>,
    preferenceManager: PreferenceRepository,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        getSettings()
            .map { rootCategories ->
                rootCategories +
                    // Include categories one tree-level down
                    rootCategories
                        .flatMap { it.settings.orEmpty().filterIsInstance<SettingsCategoryItem>() }
            }
            .map { it.firstOrNull { categoryItem -> categoryItem.route.value == categoryKey } }
            .onEach { if (it == null) onBack() }
            .filterNotNull()
            .flatMapLatest { category ->

                _state.update { it.copy(category = category) }

                preferenceManager.allSettings(
                    category.settings.orEmpty().map { it.key },
                )
                    .onEach { preferences ->
                        _state.update { it.copy(preferences = preferences) }
                    }
            }
            .launchIn(viewModelScope)

        events.filterIsInstance<Event.SettingsCategoryClick>()
            .onEach { goToSettingsForCategory(it.category) }
            .launchIn(viewModelScope)

        events.filterIsInstance<Event.CheckedChangeClick>()
            .onEach { preferenceManager.setValueByKey(it.key, it.value) }
            .launchIn(viewModelScope)

        events.filterIsInstance<Event.IntChanged>()
            .onEach { preferenceManager.setValueByKey(it.key, it.value) }
            .launchIn(viewModelScope)

        events.filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val category: SettingsCategoryItem? = null,
        val preferences: Map<SettingsKey, Any?> = emptyMap(),
    )

    sealed interface Event {
        data class SettingsCategoryClick(val category: PreferenceCategoryKey) : Event

        data class CheckedChangeClick(val key: SettingsKey, val value: Boolean) : Event

        data class IntChanged(val key: SettingsKey, val value: Int?) : Event

        data object BackClicked : Event
    }
}
