package org.ooni.probe.di

import androidx.annotation.VisibleForTesting
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.ooni.engine.Engine
import org.ooni.engine.NetworkTypeFinder
import org.ooni.engine.OonimkallBridge
import org.ooni.engine.TaskEventMapper
import org.ooni.probe.Database
import org.ooni.probe.data.disk.DeleteFile
import org.ooni.probe.data.disk.DeleteFileOkio
import org.ooni.probe.data.disk.ReadFile
import org.ooni.probe.data.disk.ReadFileOkio
import org.ooni.probe.data.disk.WriteFile
import org.ooni.probe.data.disk.WriteFileOkio
import org.ooni.probe.data.models.AutoRunParameters
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.PreferenceCategoryKey
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.repositories.MeasurementRepository
import org.ooni.probe.data.repositories.NetworkRepository
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.data.repositories.ResultRepository
import org.ooni.probe.data.repositories.TestDescriptorRepository
import org.ooni.probe.data.repositories.UrlRepository
import org.ooni.probe.domain.BootstrapTestDescriptors
import org.ooni.probe.domain.DownloadUrls
import org.ooni.probe.domain.FetchDescriptor
import org.ooni.probe.domain.GetAutoRunSpecification
import org.ooni.probe.domain.GetBootstrapTestDescriptors
import org.ooni.probe.domain.GetDefaultTestDescriptors
import org.ooni.probe.domain.GetEnginePreferences
import org.ooni.probe.domain.GetResult
import org.ooni.probe.domain.GetResults
import org.ooni.probe.domain.GetSettings
import org.ooni.probe.domain.GetTestDescriptors
import org.ooni.probe.domain.GetTestDescriptorsBySpec
import org.ooni.probe.domain.ObserveAndConfigureAutoRun
import org.ooni.probe.domain.RunDescriptors
import org.ooni.probe.domain.RunNetTest
import org.ooni.probe.domain.SaveTestDescriptors
import org.ooni.probe.domain.SendSupportEmail
import org.ooni.probe.domain.TestRunStateManager
import org.ooni.probe.domain.UploadMissingMeasurements
import org.ooni.probe.shared.PlatformInfo
import org.ooni.probe.ui.dashboard.DashboardViewModel
import org.ooni.probe.ui.descriptor.AddDescriptorViewModel
import org.ooni.probe.ui.result.ResultViewModel
import org.ooni.probe.ui.results.ResultsViewModel
import org.ooni.probe.ui.run.RunViewModel
import org.ooni.probe.ui.running.RunningViewModel
import org.ooni.probe.ui.settings.SettingsViewModel
import org.ooni.probe.ui.settings.about.AboutViewModel
import org.ooni.probe.ui.settings.category.SettingsCategoryViewModel
import org.ooni.probe.ui.settings.proxy.ProxyViewModel
import org.ooni.probe.ui.upload.UploadMeasurementsViewModel

class Dependencies(
    val platformInfo: PlatformInfo,
    private val oonimkallBridge: OonimkallBridge,
    private val baseFileDir: String,
    private val cacheDir: String,
    private val readAssetFile: (String) -> String,
    private val databaseDriverFactory: () -> SqlDriver,
    private val networkTypeFinder: NetworkTypeFinder,
    private val buildDataStore: () -> DataStore<Preferences>,
    private val isBatteryCharging: () -> Boolean,
    private val launchUrl: (String, Map<String, String>?) -> Unit,
    // TODO: Implement startSingleRun on iOS
    startSingleRunInner: ((RunSpecification) -> Unit)? = null,
    private val configureAutoRun: suspend (AutoRunParameters) -> Unit,
) {
    // Common

    private val backgroundDispatcher = Dispatchers.IO

    // Data

    val json by lazy { buildJson() }
    private val database by lazy { buildDatabase(databaseDriverFactory) }

    private val measurementRepository by lazy {
        MeasurementRepository(database, backgroundDispatcher)
    }
    private val networkRepository by lazy { NetworkRepository(database, backgroundDispatcher) }
    private val preferenceRepository by lazy { PreferenceRepository(buildDataStore()) }
    private val resultRepository by lazy { ResultRepository(database, backgroundDispatcher) }
    private val testDescriptorRepository by lazy {
        TestDescriptorRepository(database, json, backgroundDispatcher)
    }
    private val urlRepository by lazy { UrlRepository(database, backgroundDispatcher) }

    private val readFile: ReadFile by lazy { ReadFileOkio(FileSystem.SYSTEM, baseFileDir) }
    private val writeFile: WriteFile by lazy { WriteFileOkio(FileSystem.SYSTEM, baseFileDir) }
    private val deleteFile: DeleteFile by lazy { DeleteFileOkio(FileSystem.SYSTEM, baseFileDir) }

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
            isBatteryCharging = isBatteryCharging,
            platformInfo = platformInfo,
            getEnginePreferences = getEnginePreferences::invoke,
            backgroundDispatcher = backgroundDispatcher,
        )
    }

    // Domain

    val bootstrapTestDescriptors by lazy {
        BootstrapTestDescriptors(
            getBootstrapTestDescriptors = getBootstrapTestDescriptors::invoke,
            createOrIgnoreTestDescriptors = testDescriptorRepository::createOrIgnore,
            storeUrlsByUrl = urlRepository::createOrUpdateByUrl,
            preferencesRepository = preferenceRepository,
        )
    }
    val cancelCurrentTest get() = testStateManager::cancelTestRun
    private val downloadUrls by lazy {
        DownloadUrls(
            engine::checkIn,
            urlRepository::createOrUpdateByUrl,
        )
    }
    private val fetchDescriptor by lazy {
        FetchDescriptor(
            engineHttpDo = engine::httpDo,
            json = json,
        )
    }
    val getAutoRunSpecification by lazy {
        GetAutoRunSpecification(getTestDescriptors, preferenceRepository)
    }
    private val getBootstrapTestDescriptors by lazy {
        GetBootstrapTestDescriptors(readAssetFile, json, backgroundDispatcher)
    }
    val getCurrentTestState get() = testStateManager::observeState
    private val getDefaultTestDescriptors by lazy { GetDefaultTestDescriptors() }
    private val getEnginePreferences by lazy { GetEnginePreferences(preferenceRepository) }
    private val getResults by lazy {
        GetResults(
            resultRepository.listWithNetwork(),
            getTestDescriptors.invoke(),
        )
    }
    private val getResult by lazy {
        GetResult(
            getResultById = resultRepository::getById,
            getTestDescriptors = getTestDescriptors.invoke(),
            getMeasurementsByResultId = measurementRepository::listByResultId,
        )
    }
    private val getSettings by lazy { GetSettings(preferenceRepository) }
    private val getTestDescriptors by lazy {
        GetTestDescriptors(
            getDefaultTestDescriptors = getDefaultTestDescriptors::invoke,
            listInstalledTestDescriptors = testDescriptorRepository::list,
        )
    }
    private val getTestDescriptorsBySpec by lazy {
        GetTestDescriptorsBySpec(getTestDescriptors = getTestDescriptors::invoke)
    }
    val observeAndConfigureAutoRun by lazy {
        ObserveAndConfigureAutoRun(
            backgroundDispatcher = backgroundDispatcher,
            observeSettings = preferenceRepository::allSettings,
            configureAutoRun = configureAutoRun,
        )
    }
    private fun runNetTest(spec: RunNetTest.Specification) =
        RunNetTest(
            startTest = engine::startTask,
            storeResult = resultRepository::createOrUpdate,
            setCurrentTestState = testStateManager::updateState,
            getUrlByUrl = urlRepository::getByUrl,
            storeMeasurement = measurementRepository::createOrUpdate,
            storeNetwork = networkRepository::createIfNew,
            writeFile = writeFile,
            deleteFile = deleteFile,
            json = json,
            spec = spec,
        )

    val runDescriptors by lazy {
        RunDescriptors(
            getTestDescriptorsBySpec = getTestDescriptorsBySpec::invoke,
            downloadUrls = downloadUrls::invoke,
            storeResult = resultRepository::createOrUpdate,
            getCurrentTestRunState = testStateManager.observeState(),
            setCurrentTestState = testStateManager::updateState,
            runNetTest = { runNetTest(it)() },
            observeCancelTestRun = testStateManager.observeTestRunCancels(),
            reportTestRunError = testStateManager::reportError,
            getEnginePreferences = getEnginePreferences::invoke,
        )
    }
    private val saveTestDescriptors by lazy {
        SaveTestDescriptors(
            preferencesRepository = preferenceRepository,
            createOrIgnoreTestDescriptors = testDescriptorRepository::createOrIgnore,
            storeUrlsByUrl = urlRepository::createOrUpdateByUrl,
        )
    }
    val sendSupportEmail by lazy { SendSupportEmail(platformInfo, launchUrl) }

    // TODO: Remove this when startBackgroundRun is implemented on iOS
    private val startBackgroundRun: (RunSpecification) -> Unit = startSingleRunInner ?: { spec ->
        CoroutineScope(backgroundDispatcher).launch {
            runDescriptors(spec)
        }
    }
    private val testStateManager by lazy { TestRunStateManager(resultRepository.getLatest()) }
    private val uploadMissingMeasurements by lazy {
        UploadMissingMeasurements(
            getMeasurementsNotUploaded = measurementRepository.listNotUploaded(),
            submitMeasurement = engine::submitMeasurements,
            readFile = readFile,
            deleteFile = deleteFile,
            updateMeasurement = measurementRepository::createOrUpdate,
        )
    }

    // ViewModels

    fun aboutViewModel(onBack: () -> Unit) = AboutViewModel(onBack = onBack, launchUrl = { launchUrl(it, emptyMap()) })

    fun dashboardViewModel(
        goToResults: () -> Unit,
        goToRunningTest: () -> Unit,
        goToRunTests: () -> Unit,
    ) = DashboardViewModel(
        goToResults = goToResults,
        goToRunningTest = goToRunningTest,
        goToRunTests = goToRunTests,
        getTestDescriptors = getTestDescriptors::invoke,
        observeTestRunState = testStateManager.observeState(),
        observeTestRunErrors = testStateManager.observeError(),
    )

    fun proxyViewModel(onBack: () -> Unit) = ProxyViewModel(onBack, preferenceRepository)

    fun resultsViewModel(
        goToResult: (ResultModel.Id) -> Unit,
        goToUpload: () -> Unit,
    ) = ResultsViewModel(
        goToResult = goToResult,
        goToUpload = goToUpload,
        getResults = getResults::invoke,
    )

    fun runningViewModel(
        onBack: () -> Unit,
        goToResults: () -> Unit,
    ) = RunningViewModel(
        onBack = onBack,
        goToResults = goToResults,
        observeTestRunState = testStateManager.observeState(),
        observeTestRunErrors = testStateManager.observeError(),
        cancelTestRun = testStateManager::cancelTestRun,
    )

    fun runViewModel(onBack: () -> Unit) =
        RunViewModel(
            onBack = onBack,
            startBackgroundRun = startBackgroundRun,
            getTestDescriptors = getTestDescriptors::invoke,
            preferenceRepository = preferenceRepository,
        )

    fun resultViewModel(
        resultId: ResultModel.Id,
        onBack: () -> Unit,
        goToMeasurement: (MeasurementModel.ReportId, String?) -> Unit,
    ) = ResultViewModel(
        resultId = resultId,
        onBack = onBack,
        goToMeasurement = goToMeasurement,
        getResult = getResult::invoke,
        markResultAsViewed = resultRepository::markAsViewed,
    )

    fun settingsCategoryViewModel(
        categoryKey: String,
        goToSettingsForCategory: (PreferenceCategoryKey) -> Unit,
        onBack: () -> Unit,
    ) = SettingsCategoryViewModel(
        categoryKey = categoryKey,
        onBack = onBack,
        goToSettingsForCategory = goToSettingsForCategory,
        preferenceManager = preferenceRepository,
        getSettings = getSettings::invoke,
    )

    fun settingsViewModel(goToSettingsForCategory: (PreferenceCategoryKey) -> Unit) =
        SettingsViewModel(
            goToSettingsForCategory = goToSettingsForCategory,
            sendSupportEmail = sendSupportEmail::invoke,
            getSettings = getSettings::invoke,
        )

    fun uploadMeasurementsViewModel(onClose: () -> Unit) =
        UploadMeasurementsViewModel(
            onClose = onClose,
            uploadMissingMeasurements = uploadMissingMeasurements::invoke,
        )

    fun addDescriptorViewModel(
        descriptorId: String,
        onBack: () -> Unit,
        snackbarHostState: SnackbarHostState?,
        errorMessage: String,
        cancelMessage: String,
        installCompleteMessage: String,
    ): AddDescriptorViewModel {
        val scope = CoroutineScope(backgroundDispatcher)
        return AddDescriptorViewModel(
            onCancel = {
                snackbarHostState?.let {
                    showSnackbar(
                        scope = scope,
                        snackbarHostState = it,
                        message = cancelMessage,
                    )
                }
                onBack()
            },
            onError = {
                snackbarHostState?.let {
                    showSnackbar(
                        scope = scope,
                        snackbarHostState = it,
                        message = errorMessage,
                    )
                }
                onBack()
            },
            saveTestDescriptors = {
                saveTestDescriptors.invoke(it)
                snackbarHostState?.let {
                    showSnackbar(
                        scope = scope,
                        snackbarHostState = it,
                        message = installCompleteMessage,
                    )
                }
                onBack()
            },
            fetchDescriptor = {
                fetchDescriptor(descriptorId)
            },
        )
    }

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

        fun showSnackbar(
            scope: CoroutineScope,
            snackbarHostState: SnackbarHostState,
            message: String,
            actionLabel: String? = null,
            onDismissed: () -> Unit = {},
        ) {
            scope.launch {
                val result = snackbarHostState.showSnackbar(message, actionLabel)
                if (result == SnackbarResult.Dismissed) {
                    onDismissed()
                }
            }
        }
    }
}
