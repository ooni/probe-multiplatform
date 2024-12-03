package org.ooni.probe.ui.settings.webcategories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.ui.shared.SelectableItem

class WebCategoriesViewModel(
    onBack: () -> Unit,
    getPreferencesByKeys: (List<SettingsKey>) -> Flow<Map<SettingsKey, Any?>>,
    setPreferenceValuesByKeys: suspend (List<Pair<SettingsKey, Any?>>) -> Unit,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        val categories = WebConnectivityCategory.entries.filter { it.settingsKey != null }

        getPreferencesByKeys(categories.mapNotNull { it.settingsKey })
            .onEach { preferences ->
                _state.update {
                    it.copy(
                        items = categories.map { category ->
                            SelectableItem(
                                item = category,
                                isSelected = preferences[category.settingsKey] == true,
                            )
                        },
                    )
                }
            }
            .launchIn(viewModelScope)

        events.filterIsInstance<Event.PreferenceChanged>()
            .onEach {
                setPreferenceValuesByKeys(
                    listOf((it.category.settingsKey ?: return@onEach) to it.value),
                )
            }
            .launchIn(viewModelScope)

        events.filterIsInstance<Event.SelectAllClicked>()
            .onEach {
                setPreferenceValuesByKeys(
                    categories.map { (it.settingsKey ?: return@onEach) to true },
                )
            }
            .launchIn(viewModelScope)

        events.filterIsInstance<Event.DeselectAllClicked>()
            .onEach {
                setPreferenceValuesByKeys(
                    categories.map { (it.settingsKey ?: return@onEach) to false },
                )
            }
            .launchIn(viewModelScope)

        events.filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val items: List<SelectableItem<WebConnectivityCategory>> = emptyList(),
    ) {
        val selectAllEnabled get() = items.any { !it.isSelected }
        val deselectAllEnabled get() = items.any { it.isSelected }
    }

    sealed interface Event {
        data class PreferenceChanged(
            val category: WebConnectivityCategory,
            val value: Boolean,
        ) : Event

        data object SelectAllClicked : Event

        data object DeselectAllClicked : Event

        data object BackClicked : Event
    }
}
