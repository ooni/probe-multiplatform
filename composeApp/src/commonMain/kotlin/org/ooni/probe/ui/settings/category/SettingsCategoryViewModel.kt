package org.ooni.probe.ui.settings.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.update
import org.ooni.probe.config.BatteryOptimization
import org.ooni.probe.data.models.PreferenceCategoryKey
import org.ooni.probe.data.models.SettingsCategoryItem
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.ui.dashboard.DashboardViewModel.Event

class SettingsCategoryViewModel(
    categoryKey: String,
    goToSettingsForCategory: (PreferenceCategoryKey) -> Unit,
    onBack: () -> Unit,
    getSettings: () -> Flow<List<SettingsCategoryItem>>,
    preferenceRepository: PreferenceRepository,
    batteryOptimization: BatteryOptimization,
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

                preferenceRepository.allSettings(
                    category.settings.orEmpty().map { it.key },
                )
                    .onEach { preferences ->
                        _state.update { it.copy(preferences = preferences) }
                    }
            }
            .launchIn(viewModelScope)

        // When auto-run is switch to enabled, check if we're ignoring battery optimization
        preferenceRepository
            .getValueByKey(SettingsKey.AUTOMATED_TESTING_ENABLED)
            .runningFold(
                initial = null as Pair<Any?, Any?>?,
            ) { accumulator, value -> Pair(accumulator?.second, value) }
            .filterNotNull()
            .drop(1) // We only care about the first change of values
            .onEach { (previousValue, currentValue) ->
                if (previousValue != true && currentValue == true &&
                    batteryOptimization.isSupported && !batteryOptimization.isIgnoring
                ) {
                    _state.update { it.copy(showIgnoreBatteryOptimizationNotice = true) }
                }
            }
            .launchIn(viewModelScope)

        events.filterIsInstance<Event.SettingsCategoryClick>()
            .onEach { goToSettingsForCategory(it.category) }
            .launchIn(viewModelScope)

        events.filterIsInstance<Event.CheckedChangeClick>()
            .onEach { preferenceRepository.setValueByKey(it.key, it.value) }
            .launchIn(viewModelScope)

        events.filterIsInstance<Event.IntChanged>()
            .onEach { preferenceRepository.setValueByKey(it.key, it.value) }
            .launchIn(viewModelScope)

        events.filterIsInstance<Event.BackClicked>()
            .onEach { onBack() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.IgnoreBatteryOptimizationAccepted>()
            .onEach {
                _state.update { it.copy(showIgnoreBatteryOptimizationNotice = false) }
                if (batteryOptimization.isSupported && !batteryOptimization.isIgnoring) {
                    batteryOptimization.requestIgnore()
                }
            }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.IgnoreBatteryOptimizationDismissed>()
            .onEach { _state.update { it.copy(showIgnoreBatteryOptimizationNotice = false) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val category: SettingsCategoryItem? = null,
        val preferences: Map<SettingsKey, Any?> = emptyMap(),
        val showIgnoreBatteryOptimizationNotice: Boolean = false,
    )

    sealed interface Event {
        data class SettingsCategoryClick(val category: PreferenceCategoryKey) : Event

        data class CheckedChangeClick(val key: SettingsKey, val value: Boolean) : Event

        data class IntChanged(val key: SettingsKey, val value: Int?) : Event

        data object BackClicked : Event

        data object IgnoreBatteryOptimizationAccepted : Event

        data object IgnoreBatteryOptimizationDismissed : Event
    }
}
