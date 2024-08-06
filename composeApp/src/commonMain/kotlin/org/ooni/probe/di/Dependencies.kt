package org.ooni.probe.di

import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.serialization.json.Json
import org.ooni.engine.Engine
import org.ooni.engine.NetworkTypeFinder
import org.ooni.engine.OonimkallBridge
import org.ooni.engine.TaskEventMapper
import org.ooni.engine.models.NetworkType
import org.ooni.probe.data.SettingsRepository
import org.ooni.probe.data.models.TestResult
import org.ooni.probe.shared.PlatformInfo
import org.ooni.probe.ui.dashboard.DashboardViewModel
import org.ooni.probe.ui.result.ResultViewModel
import org.ooni.probe.ui.results.ResultsViewModel
import org.ooni.probe.ui.settings.SettingsViewModel
import org.ooni.probe.ui.settings.category.SettingsCategoryViewModel

class Dependencies(
    val platformInfo: PlatformInfo,
    private val oonimkallBridge: OonimkallBridge,
    private val baseFileDir: String,
    private val cacheDir: String,
    private val dataStore: DataStore<Preferences>,
) {
    // Commong

    private val backgroundDispatcher = Dispatchers.IO

    // Data

    private val json by lazy { buildJson() }

    // Engine

    private val networkTypeFinder by lazy { NetworkTypeFinder { NetworkType.Unknown("") } } // TODO

    private val taskEventMapper by lazy { TaskEventMapper(networkTypeFinder, json) }

    private val engine by lazy {
        Engine(
            bridge = oonimkallBridge,
            json = json,
            baseFilePath = baseFileDir,
            cacheDir = cacheDir,
            taskEventMapper = taskEventMapper,
            networkTypeFinder = networkTypeFinder,
            platformInfo = platformInfo,
            backgroundDispatcher = backgroundDispatcher,
        )
    }

    private val preferenceManager by lazy { SettingsRepository(dataStore) }

    // ViewModels

    val dashboardViewModel get() = DashboardViewModel(engine)

    fun resultsViewModel(goToResult: (TestResult.Id) -> Unit) = ResultsViewModel(goToResult)

    fun settingsViewModel(goToSettingsForCategory: (String) -> Unit) = SettingsViewModel(goToSettingsForCategory)

    fun settingsCategoryViewModel(
        goToSettingsForCategory: (String) -> Unit,
        onBack: () -> Unit,
    ) = SettingsCategoryViewModel(
        preferenceManager = preferenceManager,
        onBack = onBack,
        goToSettingsForCategory = goToSettingsForCategory,
    )

    fun resultViewModel(
        resultId: TestResult.Id,
        onBack: () -> Unit,
    ) = ResultViewModel(resultId, onBack)

    companion object {
        @VisibleForTesting
        fun buildJson() =
            Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
            }
    }
}
