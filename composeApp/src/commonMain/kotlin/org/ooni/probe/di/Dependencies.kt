package org.ooni.probe.di

import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import org.ooni.engine.Engine
import org.ooni.engine.NetworkTypeFinder
import org.ooni.engine.OonimkallBridge
import org.ooni.engine.TaskEventMapper
import org.ooni.probe.Database
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.repositories.PreferenceCategoryKey
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.data.repositories.ResultRepository
import org.ooni.probe.data.repositories.TestDescriptorRepository
import org.ooni.probe.domain.BootstrapTestDescriptors
import org.ooni.probe.domain.GetBootstrapTestDescriptors
import org.ooni.probe.domain.GetDefaultTestDescriptors
import org.ooni.probe.domain.GetResult
import org.ooni.probe.domain.GetResults
import org.ooni.probe.domain.GetTestDescriptors
import org.ooni.probe.shared.PlatformInfo
import org.ooni.probe.ui.dashboard.DashboardViewModel
import org.ooni.probe.ui.result.ResultViewModel
import org.ooni.probe.ui.results.ResultsViewModel
import org.ooni.probe.ui.settings.SettingsCategoryItem
import org.ooni.probe.ui.settings.SettingsViewModel
import org.ooni.probe.ui.settings.category.SettingsCategoryViewModel

class Dependencies(
    val platformInfo: PlatformInfo,
    private val oonimkallBridge: OonimkallBridge,
    private val baseFileDir: String,
    private val cacheDir: String,
    private val readAssetFile: (String) -> String,
    private val databaseDriverFactory: () -> SqlDriver,
    private val networkTypeFinder: NetworkTypeFinder,
    private val buildDataStore: () -> DataStore<Preferences>,
) {
    // Common

    private val backgroundDispatcher = Dispatchers.IO

    // Data

    private val json by lazy { buildJson() }
    private val database by lazy { buildDatabase(databaseDriverFactory) }
    private val resultRepository by lazy { ResultRepository(database, backgroundDispatcher) }
    private val testDescriptorRepository by lazy {
        TestDescriptorRepository(database, json, backgroundDispatcher)
    }

    // Engine

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

    // Domain

    val bootstrapTestDescriptors by lazy {
        BootstrapTestDescriptors(
            getBootstrapTestDescriptors = getBootstrapTestDescriptors::invoke,
            createOrIgnoreTestDescriptors = testDescriptorRepository::createOrIgnore,
        )
    }
    private val getBootstrapTestDescriptors by lazy {
        GetBootstrapTestDescriptors(readAssetFile, json, backgroundDispatcher)
    }
    private val getDefaultTestDescriptors by lazy { GetDefaultTestDescriptors() }
    private val getResults by lazy { GetResults(resultRepository) }
    private val getResult by lazy { GetResult(resultRepository) }
    private val getTestDescriptors by lazy {
        GetTestDescriptors(
            getDefaultTestDescriptors = getDefaultTestDescriptors::invoke,
            listInstalledTestDescriptors = testDescriptorRepository::list,
        )
    }
    private val preferenceManager by lazy { PreferenceRepository(buildDataStore()) }

    // ViewModels

    val dashboardViewModel
        get() =
            DashboardViewModel(
                engine = engine,
                getTestDescriptors = getTestDescriptors::invoke,
            )

    fun resultsViewModel(goToResult: (ResultModel.Id) -> Unit) = ResultsViewModel(goToResult, getResults::invoke)

    fun settingsViewModel(goToSettingsForCategory: (PreferenceCategoryKey) -> Unit) = SettingsViewModel(goToSettingsForCategory)

    fun settingsCategoryViewModel(
        goToSettingsForCategory: (PreferenceCategoryKey) -> Unit,
        onBack: () -> Unit,
        category: SettingsCategoryItem,
    ) = SettingsCategoryViewModel(
        preferenceManager = preferenceManager,
        onBack = onBack,
        goToSettingsForCategory = goToSettingsForCategory,
        category = category,
    )

    fun resultViewModel(
        resultId: ResultModel.Id,
        onBack: () -> Unit,
    ) = ResultViewModel(resultId, onBack, getResult::invoke)

    companion object {
        @VisibleForTesting
        fun buildJson() =
            Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
            }

        @VisibleForTesting
        fun buildDatabase(driverFactory: () -> SqlDriver): Database = Database(driverFactory())

        private lateinit var dataStore: DataStore<Preferences>
        internal const val DATA_STORE_FILE_NAME = "probe.preferences_pb"

        /**
         * Gets the singleton DataStore instance, creating it if necessary.
         */
        fun getDataStore(
            producePath: () -> String,
            migrations: List<DataMigration<Preferences>> = listOf(),
        ): DataStore<Preferences> =
            if (::dataStore.isInitialized) {
                dataStore
            } else {
                PreferenceDataStoreFactory.createWithPath(
                    produceFile = { producePath().toPath() },
                    migrations = migrations,
                )
                    .also { dataStore = it }
            }
    }
}
