package org.ooni.probe.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.ooni.probe.data.repositories.PreferenceCategoryKey

open class SettingsViewModel(
    goToSettingsForCategory: (PreferenceCategoryKey) -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    init {
        events.filterIsInstance<Event.SettingsCategoryClick>()
            .onEach { goToSettingsForCategory(it.category) }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    sealed interface Event {
        data class SettingsCategoryClick(val category: PreferenceCategoryKey) : Event
    }
}
