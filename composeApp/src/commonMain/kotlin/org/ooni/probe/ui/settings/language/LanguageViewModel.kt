package org.ooni.probe.ui.settings.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.ooni.probe.data.models.SettingsKey

class LanguageViewModel(
    onBack: () -> Unit,
    supportedLanguages: List<String>,
    getLanguageName: (String) -> String,
    getPreference: (SettingsKey) -> Flow<Any?>,
    private val setPreference: suspend (SettingsKey, Any?) -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        val options = supportedLanguages
            .map { code -> Option(code, getLanguageName(code)) }
            .sortedBy { it.name.lowercase() }

        getPreference(SettingsKey.LANGUAGE_SETTING)
            .map { it as? String }
            .onEach { selected ->
                _state.value = State(
                    options = options,
                    selectedLanguage = selected?.takeIf { it.isNotBlank() },
                )
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.OptionSelected>()
            .onEach { setPreference(SettingsKey.LANGUAGE_SETTING, it.code.orEmpty()) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val options: List<Option> = emptyList(),
        // null == follow the system locale ("System default")
        val selectedLanguage: String? = null,
    )

    data class Option(
        val code: String,
        val name: String,
    )

    sealed interface Event {
        data object BackClicked : Event

        // code == null selects "System default"
        data class OptionSelected(
            val code: String?,
        ) : Event
    }
}
