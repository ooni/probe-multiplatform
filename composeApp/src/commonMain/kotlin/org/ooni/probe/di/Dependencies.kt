package org.ooni.probe.di

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.unit.LayoutDirection
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.ooni.engine.Engine
import org.ooni.engine.NetworkTypeFinder
import org.ooni.engine.OonimkallBridge
import org.ooni.engine.TaskEventMapper
import org.ooni.probe.Database
import org.ooni.probe.background.RunBackgroundTask
import org.ooni.probe.config.BatteryOptimization
import org.ooni.probe.config.FlavorConfigInterface
import org.ooni.probe.data.disk.DeleteFiles
import org.ooni.probe.data.disk.DeleteFilesOkio
import org.ooni.probe.data.disk.ReadFile
import org.ooni.probe.data.disk.ReadFileOkio
import org.ooni.probe.data.disk.WriteFile
import org.ooni.probe.data.disk.WriteFileOkio
import org.ooni.probe.data.models.AutoRunParameters
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.PlatformAction
import org.ooni.probe.data.models.PreferenceCategoryKey
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.repositories.MeasurementRepository
import org.ooni.probe.data.repositories.NetworkRepository
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.data.repositories.ResultRepository
import org.ooni.probe.data.repositories.TestDescriptorRepository
import org.ooni.probe.data.repositories.UrlRepository
import org.ooni.probe.domain.BootstrapPreferences
import org.ooni.probe.domain.BootstrapTestDescriptors
import org.ooni.probe.domain.CheckSkipAutoRunNotUploadedLimit
import org.ooni.probe.domain.ClearStorage
import org.ooni.probe.domain.DeleteAllResults
import org.ooni.probe.domain.DeleteTestDescriptor
import org.ooni.probe.domain.DownloadUrls
import org.ooni.probe.domain.FetchDescriptor
import org.ooni.probe.domain.FetchDescriptorUpdate
import org.ooni.probe.domain.FinishInProgressData
import org.ooni.probe.domain.GetAutoRunSettings
import org.ooni.probe.domain.GetAutoRunSpecification
import org.ooni.probe.domain.GetBootstrapTestDescriptors
import org.ooni.probe.domain.GetDefaultTestDescriptors
import org.ooni.probe.domain.GetEnginePreferences
import org.ooni.probe.domain.GetFirstRun
import org.ooni.probe.domain.GetProxySettings
import org.ooni.probe.domain.GetResult
import org.ooni.probe.domain.GetResults
import org.ooni.probe.domain.GetSettings
import org.ooni.probe.domain.GetStorageUsed
import org.ooni.probe.domain.GetTestDescriptors
import org.ooni.probe.domain.GetTestDescriptorsBySpec
import org.ooni.probe.domain.MarkJustFinishedTestAsSeen
import org.ooni.probe.domain.ObserveAndConfigureAutoRun
import org.ooni.probe.domain.RunBackgroundStateManager
import org.ooni.probe.domain.RunDescriptors
import org.ooni.probe.domain.RunNetTest
import org.ooni.probe.domain.SaveTestDescriptors
import org.ooni.probe.domain.SendSupportEmail
import org.ooni.probe.domain.ShareLogFile
import org.ooni.probe.domain.ShouldShowVpnWarning
import org.ooni.probe.domain.UploadMissingMeasurements
import org.ooni.probe.shared.PlatformInfo
import org.ooni.probe.shared.monitoring.AppLogger
import org.ooni.probe.shared.monitoring.CrashMonitoring
import org.ooni.probe.ui.choosewebsites.ChooseWebsitesViewModel
import org.ooni.probe.ui.dashboard.DashboardViewModel
import org.ooni.probe.ui.descriptor.DescriptorViewModel
import org.ooni.probe.ui.descriptor.add.AddDescriptorViewModel
import org.ooni.probe.ui.descriptor.review.ReviewUpdatesViewModel
import org.ooni.probe.ui.log.LogViewModel
import org.ooni.probe.ui.measurement.MeasurementViewModel
import org.ooni.probe.ui.onboarding.OnboardingViewModel
import org.ooni.probe.ui.result.ResultViewModel
import org.ooni.probe.ui.results.ResultsViewModel
import org.ooni.probe.ui.run.RunViewModel
import org.ooni.probe.ui.running.RunningViewModel
import org.ooni.probe.ui.settings.SettingsViewModel
import org.ooni.probe.ui.settings.about.AboutViewModel
import org.ooni.probe.ui.settings.category.SettingsCategoryViewModel
import org.ooni.probe.ui.settings.proxy.ProxyViewModel
import org.ooni.probe.ui.settings.webcategories.WebCategoriesViewModel
import org.ooni.probe.ui.upload.UploadMeasurementsViewModel
import kotlin.coroutines.CoroutineContext

class Dependencies(
    val platformInfo: PlatformInfo,
    private val oonimkallBridge: OonimkallBridge,
    private val baseFileDir: String,
    private val cacheDir: String,
    private val readAssetFile: (String) -> String,
    private val databaseDriverFactory: () -> SqlDriver,
    private val networkTypeFinder: NetworkTypeFinder,
    @VisibleForTesting
    val buildDataStore: () -> DataStore<Preferences>,
    private val isBatteryCharging: () -> Boolean,
    val startSingleRunInner: (RunSpecification) -> Unit,
    private val configureAutoRun: suspend (AutoRunParameters) -> Unit,
    val configureDescriptorAutoUpdate: suspend () -> Boolean,
    val fetchDescriptorUpdate: suspend (List<InstalledTestDescriptorModel>?) -> Unit,
    val localeDirection: (() -> LayoutDirection)? = null,
    private val launchAction: (PlatformAction) -> Boolean,
    private val batteryOptimization: BatteryOptimization,
    val flavorConfig: FlavorConfigInterface,
) {
    // Common

    @VisibleForTesting
    var backgroundContext: CoroutineContext = Dispatchers.IO

    // Data

    val json by lazy { buildJson() }
    private val database by lazy { buildDatabase(databaseDriverFactory) }

    private val measurementRepository by lazy {
        MeasurementRepository(database, json, backgroundContext)
    }
    private val networkRepository by lazy { NetworkRepository(database, backgroundContext) }

    @VisibleForTesting
    val preferenceRepository by lazy { PreferenceRepository(buildDataStore()) }
    private val resultRepository by lazy { ResultRepository(database, backgroundContext) }
    val testDescriptorRepository by lazy {
        TestDescriptorRepository(database, json, backgroundContext)
    }
    private val urlRepository by lazy { UrlRepository(database, backgroundContext) }

    private val readFile: ReadFile by lazy { ReadFileOkio(FileSystem.SYSTEM, baseFileDir) }
    private val writeFile: WriteFile by lazy { WriteFileOkio(FileSystem.SYSTEM, baseFileDir) }
    private val deleteFiles: DeleteFiles by lazy { DeleteFilesOkio(FileSystem.SYSTEM, baseFileDir) }

    private val getStorageUsed by lazy {
        GetStorageUsed(
            backgroundContext = backgroundContext,
            baseFileDir = baseFileDir,
            cacheDir = cacheDir,
            fileSystem = FileSystem.SYSTEM,
        )
    }

    // Monitoring

    val crashMonitoring by lazy { CrashMonitoring(preferenceRepository, platformInfo) }
    val appLogger by lazy {
        AppLogger(
            readFile = readFile,
            writeFile = writeFile,
            deleteFiles = deleteFiles,
            backgroundContext = backgroundContext,
        )
    }

    // Engine

    private val taskEventMapper by lazy { TaskEventMapper(networkTypeFinder, json) }

    @VisibleForTesting
    val engine by lazy {
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
            addRunCancelListener = runBackgroundStateManager::addCancelListener,
            backgroundContext = backgroundContext,
        )
    }

    // Domain

    val bootstrapPreferences by lazy {
        BootstrapPreferences(preferenceRepository, getTestDescriptors::latest)
    }
    val bootstrapTestDescriptors by lazy {
        BootstrapTestDescriptors(
            getBootstrapTestDescriptors = getBootstrapTestDescriptors::invoke,
            saveTestDescriptors = saveTestDescriptors::invoke,
        )
    }
    val cancelCurrentTest get() = runBackgroundStateManager::cancel
    private val downloadUrls by lazy {
        DownloadUrls(
            engine::checkIn,
            urlRepository::createOrUpdateByUrl,
        )
    }
    private val checkSkipAutoRunNotUploadedLimit by lazy {
        CheckSkipAutoRunNotUploadedLimit(
            resultRepository::countMissingUpload,
        )
    }
    private val deleteAllResults by lazy {
        DeleteAllResults(resultRepository::deleteAll, deleteFiles::invoke)
    }
    private val deleteTestDescriptor by lazy {
        DeleteTestDescriptor(
            preferencesRepository = preferenceRepository,
            deleteByRunId = testDescriptorRepository::deleteByRunId,
            deleteMeasurementByResultRunId = measurementRepository::deleteByResultRunId,
            selectMeasurementsByResultRunId = measurementRepository::selectByResultRunId,
            deleteResultByRunId = resultRepository::deleteByRunId,
            deleteFile = deleteFiles::invoke,
        )
    }
    private val fetchDescriptor by lazy {
        FetchDescriptor(
            engineHttpDo = engine::httpDo,
            json = json,
        )
    }
    val finishInProgressData by lazy { FinishInProgressData(resultRepository::markAllAsDone) }
    val getDescriptorUpdate by lazy {
        FetchDescriptorUpdate(
            fetchDescriptor = fetchDescriptor::invoke,
            saveTestDescriptors = saveTestDescriptors::invoke,
            listInstalledTestDescriptors = testDescriptorRepository::listLatest,
        )
    }
    val getAutoRunSettings by lazy { GetAutoRunSettings(preferenceRepository::allSettings) }
    private val getAutoRunSpecification by lazy {
        GetAutoRunSpecification(getTestDescriptors::latest, preferenceRepository)
    }
    private val getBootstrapTestDescriptors by lazy {
        GetBootstrapTestDescriptors(readAssetFile, json, backgroundContext)
    }
    private val getDefaultTestDescriptors by lazy { GetDefaultTestDescriptors() }
    private val getProxySettings by lazy { GetProxySettings(preferenceRepository) }
    private val getEnginePreferences by lazy {
        GetEnginePreferences(
            preferencesRepository = preferenceRepository,
            getProxySettings = getProxySettings::invoke,
        )
    }
    private val getFirstRun by lazy { GetFirstRun(preferenceRepository) }
    private val getResults by lazy {
        GetResults(
            resultRepository::list,
            getTestDescriptors::all,
            measurementRepository::selectTestKeysByDescriptorKey,
        )
    }
    private val getResult by lazy {
        GetResult(
            getResultById = resultRepository::getById,
            getTestDescriptors = getTestDescriptors::all,
            getMeasurementsByResultId = measurementRepository::listByResultId,
            getTestKeys = measurementRepository::selectTestKeysByResultId,
        )
    }
    private val clearStorage by lazy {
        ClearStorage(
            backgroundContext = backgroundContext,
            deleteAllResults = deleteAllResults::invoke,
            clearLogs = appLogger::clear,
            updateStorageUsed = getStorageUsed::update,
            clearPreferences = preferenceRepository::clear,
        )
    }

    private val getSettings by lazy {
        GetSettings(
            preferencesRepository = preferenceRepository,
            observeStorageUsed = getStorageUsed::observe,
            clearStorage = clearStorage::invoke,
            supportsCrashReporting = flavorConfig.isCrashReportingEnabled,
        )
    }

    @VisibleForTesting
    val getTestDescriptors by lazy {
        GetTestDescriptors(
            getDefaultTestDescriptors = getDefaultTestDescriptors::invoke,
            listAllInstalledTestDescriptors = testDescriptorRepository::listAll,
            listLatestInstalledTestDescriptors = testDescriptorRepository::listLatest,
            descriptorUpdates = getDescriptorUpdate::observeAvailableUpdatesState,
            getPreferenceValues = preferenceRepository::allSettings,
        )
    }
    private val getTestDescriptorsBySpec by lazy {
        GetTestDescriptorsBySpec(getTestDescriptors = getTestDescriptors::latest)
    }
    private val markJustFinishedTestAsSeen by lazy {
        MarkJustFinishedTestAsSeen(runBackgroundStateManager::updateState)
    }
    val observeAndConfigureAutoRun by lazy {
        ObserveAndConfigureAutoRun(
            backgroundContext = backgroundContext,
            configureAutoRun = configureAutoRun,
            getAutoRunSettings = getAutoRunSettings::invoke,
        )
    }
    private val runDescriptors by lazy {
        RunDescriptors(
            getTestDescriptorsBySpec = getTestDescriptorsBySpec::invoke,
            downloadUrls = downloadUrls::invoke,
            storeResult = resultRepository::createOrUpdate,
            markResultAsDone = resultRepository::markAsDone,
            getRunBackgroundState = runBackgroundStateManager.observeState(),
            setRunBackgroundState = runBackgroundStateManager::updateState,
            runNetTest = { runNetTest(it)() },
            addRunCancelListener = runBackgroundStateManager::addCancelListener,
            reportTestRunError = runBackgroundStateManager::reportError,
            getEnginePreferences = getEnginePreferences::invoke,
            finishInProgressData = finishInProgressData::invoke,
        )
    }
    private val saveTestDescriptors by lazy {
        SaveTestDescriptors(
            createOrIgnoreDescriptors = testDescriptorRepository::createOrIgnore,
            createOrUpdateDescriptors = testDescriptorRepository::createOrUpdate,
            storeUrlsByUrl = urlRepository::createOrUpdateByUrl,
        )
    }
    private val sendSupportEmail by lazy { SendSupportEmail(platformInfo, launchAction) }
    private val shareLogFile by lazy { ShareLogFile(launchAction, appLogger::getLogFilePath) }
    private val shouldShowVpnWarning by lazy {
        ShouldShowVpnWarning(preferenceRepository, networkTypeFinder::invoke)
    }
    val runBackgroundStateManager by lazy { RunBackgroundStateManager(resultRepository.getLatest()) }
    private val uploadMissingMeasurements by lazy {
        UploadMissingMeasurements(
            getMeasurementsNotUploaded = measurementRepository::listNotUploaded,
            submitMeasurement = engine::submitMeasurements,
            readFile = readFile,
            deleteFiles = deleteFiles,
            updateMeasurement = measurementRepository::createOrUpdate,
        )
    }

    private fun runNetTest(spec: RunNetTest.Specification) =
        RunNetTest(
            startTest = engine::startTask,
            getResultByIdAndUpdate = resultRepository::getByIdAndUpdate,
            setCurrentTestState = runBackgroundStateManager::updateState,
            getOrCreateUrl = urlRepository::getOrCreateByUrl,
            storeMeasurement = measurementRepository::createOrUpdate,
            storeNetwork = networkRepository::createIfNew,
            writeFile = writeFile,
            deleteFiles = deleteFiles,
            json = json,
            spec = spec,
        )

    // Background

    val runBackgroundTask by lazy {
        RunBackgroundTask(
            getPreferenceValueByKey = preferenceRepository::getValueByKey,
            uploadMissingMeasurements = uploadMissingMeasurements::invoke,
            checkSkipAutoRunNotUploadedLimit = checkSkipAutoRunNotUploadedLimit::invoke,
            getNetworkType = networkTypeFinder::invoke,
            getAutoRunSpecification = getAutoRunSpecification::invoke,
            runDescriptors = runDescriptors::invoke,
            addRunCancelListener = runBackgroundStateManager::addCancelListener,
            clearRunCancelListeners = runBackgroundStateManager::clearCancelListeners,
            setRunBackgroundState = runBackgroundStateManager::updateState,
            getRunBackgroundState = runBackgroundStateManager::observeState,
            getLatestResult = resultRepository::getLatest,
        )
    }

    // ViewModels

    fun aboutViewModel(onBack: () -> Unit) =
        AboutViewModel(onBack = onBack, launchUrl = {
            launchAction(PlatformAction.OpenUrl(it))
        }, platformInfo = platformInfo)

    fun addDescriptorViewModel(
        descriptorId: String,
        onBack: () -> Unit,
    ) = AddDescriptorViewModel(
        onBack = onBack,
        saveTestDescriptors = saveTestDescriptors::invoke,
        fetchDescriptor = { fetchDescriptor(descriptorId) },
        preferenceRepository = preferenceRepository,
    )

    fun chooseWebsitesViewModel(
        onBack: () -> Unit,
        goToDashboard: () -> Unit,
        initialUrl: String?,
    ) = ChooseWebsitesViewModel(
        onBack = onBack,
        goToDashboard = goToDashboard,
        startBackgroundRun = startSingleRunInner,
        initialUrl = initialUrl,
    )

    fun dashboardViewModel(
        goToOnboarding: () -> Unit,
        goToResults: () -> Unit,
        goToRunningTest: () -> Unit,
        goToRunTests: () -> Unit,
        goToDescriptor: (String) -> Unit,
        goToReviewDescriptorUpdates: () -> Unit,
    ) = DashboardViewModel(
        goToOnboarding = goToOnboarding,
        goToResults = goToResults,
        goToRunningTest = goToRunningTest,
        goToRunTests = goToRunTests,
        goToDescriptor = goToDescriptor,
        getFirstRun = getFirstRun::invoke,
        goToReviewDescriptorUpdates = goToReviewDescriptorUpdates,
        getTestDescriptors = getTestDescriptors::latest,
        observeRunBackgroundState = runBackgroundStateManager.observeState(),
        observeTestRunErrors = runBackgroundStateManager.observeErrors(),
        shouldShowVpnWarning = shouldShowVpnWarning::invoke,
        fetchDescriptorUpdate = fetchDescriptorUpdate,
        observeAvailableUpdatesState = getDescriptorUpdate::observeAvailableUpdatesState,
        reviewUpdates = getDescriptorUpdate::reviewUpdates,
        cancelUpdates = getDescriptorUpdate::cancelUpdates,
    )

    fun descriptorViewModel(
        descriptorKey: String,
        onBack: () -> Unit,
        goToReviewDescriptorUpdates: () -> Unit,
        goToChooseWebsites: () -> Unit,
    ) = DescriptorViewModel(
        descriptorKey = descriptorKey,
        onBack = onBack,
        goToReviewDescriptorUpdates = goToReviewDescriptorUpdates,
        goToChooseWebsites = goToChooseWebsites,
        getLatestTestDescriptors = getTestDescriptors::latest,
        getDescriptorLastResult = resultRepository::getLatestByDescriptor,
        preferenceRepository = preferenceRepository,
        launchUrl = { launchAction(PlatformAction.OpenUrl(it)) },
        deleteTestDescriptor = deleteTestDescriptor::invoke,
        fetchDescriptorUpdate = fetchDescriptorUpdate,
        setAutoUpdate = testDescriptorRepository::setAutoUpdate,
        reviewUpdates = getDescriptorUpdate::reviewUpdates,
        descriptorUpdates = getDescriptorUpdate::observeAvailableUpdatesState,
    )

    fun logViewModel(onBack: () -> Unit) =
        LogViewModel(
            onBack = onBack,
            readLog = appLogger::read,
            clearLog = appLogger::clear,
            shareLogFile = shareLogFile::invoke,
        )

    fun onboardingViewModel(
        goToDashboard: () -> Unit,
        goToSettings: () -> Unit,
    ) = OnboardingViewModel(
        goToDashboard = goToDashboard,
        goToSettings = goToSettings,
        platformInfo = platformInfo,
        preferenceRepository = preferenceRepository,
        launchUrl = { launchAction(PlatformAction.OpenUrl(it)) },
        batteryOptimization = batteryOptimization,
        supportsCrashReporting = flavorConfig.isCrashReportingEnabled,
    )

    fun proxyViewModel(onBack: () -> Unit) = ProxyViewModel(onBack, preferenceRepository)

    fun resultsViewModel(
        goToResult: (ResultModel.Id) -> Unit,
        goToUpload: () -> Unit,
    ) = ResultsViewModel(
        goToResult = goToResult,
        goToUpload = goToUpload,
        getResults = getResults::invoke,
        getDescriptors = getTestDescriptors::latest,
        deleteAllResults = deleteAllResults::invoke,
        markJustFinishedTestAsSeen = markJustFinishedTestAsSeen::invoke,
    )

    fun runningViewModel(
        onBack: () -> Unit,
        goToResults: () -> Unit,
    ) = RunningViewModel(
        onBack = onBack,
        goToResults = goToResults,
        observeRunBackgroundState = runBackgroundStateManager.observeState(),
        observeTestRunErrors = runBackgroundStateManager.observeErrors(),
        cancelTestRun = runBackgroundStateManager::cancel,
        getProxySettings = getProxySettings::invoke,
    )

    fun runViewModel(onBack: () -> Unit) =
        RunViewModel(
            onBack = onBack,
            getTestDescriptors = getTestDescriptors::latest,
            shouldShowVpnWarning = shouldShowVpnWarning::invoke,
            preferenceRepository = preferenceRepository,
            startBackgroundRun = startSingleRunInner,
            openVpnSettings = launchAction,
        )

    fun resultViewModel(
        resultId: ResultModel.Id,
        onBack: () -> Unit,
        goToMeasurement: (MeasurementModel.ReportId, String?) -> Unit,
        goToUpload: () -> Unit,
        goToDashboard: () -> Unit,
    ) = ResultViewModel(
        resultId = resultId,
        onBack = onBack,
        goToMeasurement = goToMeasurement,
        goToUpload = goToUpload,
        goToDashboard = goToDashboard,
        getResult = getResult::invoke,
        getCurrentRunBackgroundState = runBackgroundStateManager.observeState(),
        markResultAsViewed = resultRepository::markAsViewed,
        startBackgroundRun = startSingleRunInner,
    )

    fun measurementViewModel(onBack: () -> Unit) =
        MeasurementViewModel(
            onBack = onBack,
            shareUrl = { launchAction(PlatformAction.Share(it)) },
        )

    fun reviewUpdatesViewModel(onBack: () -> Unit): ReviewUpdatesViewModel {
        return ReviewUpdatesViewModel(
            onBack = onBack,
            saveTestDescriptors = saveTestDescriptors::invoke,
            cancelUpdates = getDescriptorUpdate::cancelUpdates,
            observeAvailableUpdatesState = getDescriptorUpdate::observeAvailableUpdatesState,
        )
    }

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

    fun uploadMeasurementsViewModel(
        resultId: ResultModel.Id?,
        onClose: () -> Unit,
    ) = UploadMeasurementsViewModel(
        resultId = resultId,
        onClose = onClose,
        uploadMissingMeasurements = uploadMissingMeasurements::invoke,
    )

    fun webCategoriesViewModel(onBack: () -> Unit) =
        WebCategoriesViewModel(
            onBack = onBack,
            getPreferencesByKeys = preferenceRepository::allSettings,
            setPreferenceValuesByKeys = preferenceRepository::setValuesByKey,
        )

    companion object {
        @VisibleForTesting
        fun buildJson() =
            Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
                isLenient = true
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
