package org.ooni.probe.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.ooni.probe.data.models.PreferenceCategoryKey
import org.ooni.probe.data.models.SettingsCategoryItem

open class SettingsViewModel(
    goToSettingsForCategory: (PreferenceCategoryKey) -> Unit,
    openAppLanguageSettings: suspend () -> Unit,
    getSettings: () -> Flow<List<SettingsCategoryItem>>,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        getSettings()
            .onEach { _state.value = State(it) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.SettingsCategoryClick>()
            .onEach {
                when (it.category) {
                    PreferenceCategoryKey.LANGUAGE -> openAppLanguageSettings()
                    else -> goToSettingsForCategory(it.category)
                }
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val settings: List<SettingsCategoryItem> = emptyList(),
    )

    sealed interface Event {
        data class SettingsCategoryClick(
            val category: PreferenceCategoryKey,
        ) : Event
    }
}
