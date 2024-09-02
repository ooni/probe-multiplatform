package org.ooni.probe.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.ooni.probe.data.models.PreferenceCategoryKey

open class SettingsViewModel(
    goToSettingsForCategory: (PreferenceCategoryKey) -> Unit,
    sendSupportEmail: suspend () -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    init {
        events.filterIsInstance<Event.SettingsCategoryClick>()
            .onEach {
                when (it.category) {
                    PreferenceCategoryKey.SEND_EMAIL -> {
                        sendSupportEmail()
                    } else -> {
                        goToSettingsForCategory(it.category)
                    }
                }
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    sealed interface Event {
        data class SettingsCategoryClick(val category: PreferenceCategoryKey) : Event
    }
}
