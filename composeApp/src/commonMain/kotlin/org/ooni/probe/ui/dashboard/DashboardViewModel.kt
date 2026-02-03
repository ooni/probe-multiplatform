package org.ooni.probe.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import org.ooni.probe.config.BatteryOptimization
import org.ooni.probe.data.models.ArticleModel
import org.ooni.probe.data.models.AutoRunParameters
import org.ooni.probe.data.models.MeasurementStats
import org.ooni.probe.data.models.RunBackgroundState
import org.ooni.probe.data.models.RunSummary
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.models.TestRunError
import org.ooni.probe.shared.tickerFlow
import kotlin.time.Duration.Companion.seconds

class DashboardViewModel(
    goToOnboarding: () -> Unit,
    goToResults: () -> Unit,
    goToRunningTest: () -> Unit,
    goToRunTests: () -> Unit,
    goToTests: () -> Unit,
    goToTestSettings: () -> Unit,
    goToArticles: () -> Unit,
    goToArticle: (ArticleModel.Url) -> Unit,
    getFirstRun: () -> Flow<Boolean>,
    observeRunBackgroundState: () -> Flow<RunBackgroundState>,
    observeTestRunErrors: () -> Flow<TestRunError>,
    shouldShowVpnWarning: suspend () -> Boolean,
    getAutoRunSettings: () -> Flow<AutoRunParameters>,
    getLastRun: () -> Flow<RunSummary?>,
    dismissLastRun: suspend () -> Unit,
    getPreference: (SettingsKey) -> Flow<Any?>,
    setPreference: suspend (SettingsKey, Any) -> Unit,
    getStats: () -> Flow<MeasurementStats>,
    getArticles: () -> Flow<List<ArticleModel>>,
    batteryOptimization: BatteryOptimization,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        getFirstRun()
            .take(1)
            .onEach { firstRun -> if (firstRun) goToOnboarding() }
            .launchIn(viewModelScope)

        getAutoRunSettings()
            .onEach { autoRunParameters ->
                _state.update {
                    it.copy(isAutoRunEnabled = autoRunParameters is AutoRunParameters.Enabled)
                }
            }.launchIn(viewModelScope)

        getAutoRunSettings()
            .take(1)
            .onEach { autoRunParameters ->
                _state.update {
                    it.copy(
                        showIgnoreBatteryOptimizationNotice =
                            autoRunParameters is AutoRunParameters.Enabled &&
                                batteryOptimization.isSupported &&
                                !batteryOptimization.isIgnoring,
                    )
                }
            }.launchIn(viewModelScope)

        observeRunBackgroundState()
            .onEach { testState ->
                _state.update { it.copy(runBackgroundState = testState) }
            }.launchIn(viewModelScope)

        observeTestRunErrors()
            .onEach { error ->
                _state.update { it.copy(testRunErrors = it.testRunErrors + error) }
            }.launchIn(viewModelScope)

        getLastRun()
            .onEach { run ->
                _state.update { it.copy(lastRun = run) }
            }.launchIn(viewModelScope)

        getPreference(SettingsKey.TESTS_MOVED_NOTICE)
            .onEach { preference ->
                _state.update { it.copy(showTestsMovedNotice = preference != true) }
            }.launchIn(viewModelScope)

        getStats()
            .onEach { stats ->
                _state.update { it.copy(stats = stats) }
            }.launchIn(viewModelScope)

        getArticles()
            .onEach { articles ->
                _state.update {
                    it.copy(
                        articles = articles.take(ARTICLES_TO_SHOW),
                        showReadMoreArticles = articles.size > ARTICLES_TO_SHOW,
                    )
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.RunTestsClicked>()
            .onEach { goToRunTests() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.RunningTestClicked>()
            .onEach { goToRunningTest() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.AutoRunClicked>()
            .onEach { goToTestSettings() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.SeeResultsClicked>()
            .onEach { goToResults() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DismissResultsClicked>()
            .onEach { dismissLastRun() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.SeeTestsClicked>()
            .onEach { goToTests() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.DismissTestsMovedClicked>()
            .onEach { setPreference(SettingsKey.TESTS_MOVED_NOTICE, true) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ArticleClicked>()
            .onEach { goToArticle(it.article.url) }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ReadMoreArticlesClicked>()
            .onEach { goToArticles() }
            .launchIn(viewModelScope)

        events
            .filterIsInstance<Event.ErrorDisplayed>()
            .onEach { event ->
                _state.update { it.copy(testRunErrors = it.testRunErrors - event.error) }
            }.launchIn(viewModelScope)

        merge(
            events.filterIsInstance<Event.Resumed>(),
            events.filterIsInstance<Event.Paused>(),
        ).flatMapLatest {
            if (it is Event.Resumed) {
                tickerFlow(CHECK_VPN_WARNING_INTERVAL)
            } else {
                emptyFlow()
            }
        }.onEach {
            _state.update { it.copy(showVpnWarning = shouldShowVpnWarning()) }
        }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.IgnoreBatteryOptimizationAccepted>()
            .onEach {
                _state.update { it.copy(showIgnoreBatteryOptimizationNotice = false) }
                if (batteryOptimization.isSupported && !batteryOptimization.isIgnoring) {
                    batteryOptimization.requestIgnore()
                }
            }.launchIn(viewModelScope)

        events
            .filterIsInstance<Event.IgnoreBatteryOptimizationDismissed>()
            .onEach { _state.update { it.copy(showIgnoreBatteryOptimizationNotice = false) } }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    data class State(
        val runBackgroundState: RunBackgroundState = RunBackgroundState.Idle,
        val isAutoRunEnabled: Boolean = false,
        val stats: MeasurementStats? = null,
        val articles: List<ArticleModel> = emptyList(),
        val showReadMoreArticles: Boolean = false,
        val testRunErrors: List<TestRunError> = emptyList(),
        val showVpnWarning: Boolean = false,
        val lastRun: RunSummary? = null,
        val showIgnoreBatteryOptimizationNotice: Boolean = false,
        val showTestsMovedNotice: Boolean = false,
    )

    sealed interface Event {
        data object Resumed : Event

        data object Paused : Event

        data object RunTestsClicked : Event

        data object RunningTestClicked : Event

        data object AutoRunClicked : Event

        data object SeeResultsClicked : Event

        data object DismissResultsClicked : Event

        data object SeeTestsClicked : Event

        data object DismissTestsMovedClicked : Event

        data class ArticleClicked(
            val article: ArticleModel,
        ) : Event

        data object ReadMoreArticlesClicked : Event

        data class ErrorDisplayed(
            val error: TestRunError,
        ) : Event

        data object IgnoreBatteryOptimizationAccepted : Event

        data object IgnoreBatteryOptimizationDismissed : Event
    }

    companion object {
        private val CHECK_VPN_WARNING_INTERVAL = 5.seconds
        private const val ARTICLES_TO_SHOW = 3
    }
}
