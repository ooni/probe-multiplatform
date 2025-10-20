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
import org.ooni.probe.config.ProxyConfig
import org.ooni.probe.data.disk.DeleteFiles
import org.ooni.probe.data.disk.DeleteFilesOkio
import org.ooni.probe.data.disk.ReadFile
import org.ooni.probe.data.disk.ReadFileOkio
import org.ooni.probe.data.disk.WriteFile
import org.ooni.probe.data.disk.WriteFileOkio
import org.ooni.probe.data.models.AutoRunParameters
import org.ooni.probe.data.models.BatteryState
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementsFilter
import org.ooni.probe.data.models.PlatformAction
import org.ooni.probe.data.models.PreferenceCategoryKey
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.repositories.AppReviewRepository
import org.ooni.probe.data.repositories.MeasurementRepository
import org.ooni.probe.data.repositories.NetworkRepository
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.data.repositories.ResultRepository
import org.ooni.probe.data.repositories.TestDescriptorRepository
import org.ooni.probe.data.repositories.UrlRepository
import org.ooni.probe.domain.BootstrapPreferences
import org.ooni.probe.domain.CheckAutoRunConstraints
import org.ooni.probe.domain.ClearStorage
import org.ooni.probe.domain.DeleteMeasurementsWithoutResult
import org.ooni.probe.domain.DownloadUrls
import org.ooni.probe.domain.FinishInProgressData
import org.ooni.probe.domain.GetAutoRunSettings
import org.ooni.probe.domain.GetAutoRunSpecification
import org.ooni.probe.domain.GetBootstrapTestDescriptors
import org.ooni.probe.domain.GetDefaultTestDescriptors
import org.ooni.probe.domain.GetEnginePreferences
import org.ooni.probe.domain.GetFirstRun
import org.ooni.probe.domain.GetLastResultOfDescriptor
import org.ooni.probe.domain.GetMeasurementsNotUploaded
import org.ooni.probe.domain.GetSettings
import org.ooni.probe.domain.GetStats
import org.ooni.probe.domain.GetStorageUsed
import org.ooni.probe.domain.ObserveAndConfigureAutoRun
import org.ooni.probe.domain.ObserveAndConfigureAutoUpdate
import org.ooni.probe.domain.RunBackgroundStateManager
import org.ooni.probe.domain.RunDescriptors
import org.ooni.probe.domain.RunNetTest
import org.ooni.probe.domain.SendSupportEmail
import org.ooni.probe.domain.ShareLogFile
import org.ooni.probe.domain.ShouldShowVpnWarning
import org.ooni.probe.domain.UploadMissingMeasurements
import org.ooni.probe.domain.appreview.MarkAppReviewAsShown
import org.ooni.probe.domain.appreview.ShouldShowAppReview
import org.ooni.probe.domain.descriptors.AcceptDescriptorUpdate
import org.ooni.probe.domain.descriptors.BootstrapTestDescriptors
import org.ooni.probe.domain.descriptors.DeleteTestDescriptor
import org.ooni.probe.domain.descriptors.DescriptorUpdateStateManager
import org.ooni.probe.domain.descriptors.DismissDescriptorReviewNotice
import org.ooni.probe.domain.descriptors.FetchDescriptor
import org.ooni.probe.domain.descriptors.FetchDescriptorsUpdates
import org.ooni.probe.domain.descriptors.GetTestDescriptors
import org.ooni.probe.domain.descriptors.GetTestDescriptorsBySpec
import org.ooni.probe.domain.descriptors.RejectDescriptorUpdate
import org.ooni.probe.domain.descriptors.SaveTestDescriptors
import org.ooni.probe.domain.descriptors.UndoRejectedDescriptorUpdate
import org.ooni.probe.domain.proxy.ProxyManager
import org.ooni.probe.domain.proxy.TestProxy
import org.ooni.probe.domain.results.DeleteOldResults
import org.ooni.probe.domain.results.DeleteResults
import org.ooni.probe.domain.results.DismissLastRun
import org.ooni.probe.domain.results.GetLastRun
import org.ooni.probe.domain.results.GetResult
import org.ooni.probe.domain.results.GetResults
import org.ooni.probe.shared.PlatformInfo
import org.ooni.probe.shared.monitoring.AppLogger
import org.ooni.probe.shared.monitoring.CrashMonitoring
import org.ooni.probe.ui.choosewebsites.ChooseWebsitesViewModel
import org.ooni.probe.ui.dashboard.DashboardViewModel
import org.ooni.probe.ui.descriptor.DescriptorViewModel
import org.ooni.probe.ui.descriptor.add.AddDescriptorViewModel
import org.ooni.probe.ui.descriptor.review.ReviewUpdatesViewModel
import org.ooni.probe.ui.descriptor.websites.DescriptorWebsitesViewModel
import org.ooni.probe.ui.descriptors.DescriptorsViewModel
import org.ooni.probe.ui.log.LogViewModel
import org.ooni.probe.ui.measurement.MeasurementRawViewModel
import org.ooni.probe.ui.measurement.MeasurementViewModel
import org.ooni.probe.ui.navigation.BottomBarViewModel
import org.ooni.probe.ui.onboarding.OnboardingViewModel
import org.ooni.probe.ui.result.ResultViewModel
import org.ooni.probe.ui.results.ResultsViewModel
import org.ooni.probe.ui.run.RunViewModel
import org.ooni.probe.ui.running.RunningViewModel
import org.ooni.probe.ui.settings.SettingsViewModel
import org.ooni.probe.ui.settings.about.AboutViewModel
import org.ooni.probe.ui.settings.category.SettingsCategoryViewModel
import org.ooni.probe.ui.settings.proxy.AddProxyViewModel
import org.ooni.probe.ui.settings.proxy.ProxyViewModel
import org.ooni.probe.ui.settings.webcategories.WebCategoriesViewModel
import org.ooni.probe.ui.upload.UploadMeasurementsViewModel
import kotlin.coroutines.CoroutineContext

class Dependencies(
    val platformInfo: PlatformInfo,
    private val oonimkallBridge: OonimkallBridge,
    private val baseFileDir: String,
    val cacheDir: String,
    private val readAssetFile: (String) -> String,
    private val databaseDriverFactory: () -> SqlDriver,
    private val networkTypeFinder: NetworkTypeFinder,
    @get:VisibleForTesting
    val buildDataStore: () -> DataStore<Preferences>,
    private val getBatteryState: () -> BatteryState,
    val startSingleRunInner: (RunSpecification) -> Unit,
    private val configureAutoRun: suspend (AutoRunParameters) -> Unit,
    val configureDescriptorAutoUpdate: suspend () -> Boolean,
    val cancelDescriptorAutoUpdate: suspend () -> Boolean,
    val startDescriptorsUpdate: suspend (List<InstalledTestDescriptorModel>?) -> Unit,
    val localeDirection: (() -> LayoutDirection)? = null,
    private val isWebViewAvailable: () -> Boolean,
    private val isCleanUpRequired: () -> Boolean = { false },
    val launchAction: (PlatformAction) -> Boolean,
    val cleanupLegacyDirectories: (suspend () -> Boolean)? = null,
    private val batteryOptimization: BatteryOptimization,
    val flavorConfig: FlavorConfigInterface,
    val proxyConfig: ProxyConfig,
) {
    // Common

    @VisibleForTesting
    var backgroundContext: CoroutineContext = Dispatchers.IO

    // Data

    val json by lazy { buildJson() }
    private val database by lazy { buildDatabase(databaseDriverFactory) }

    private val appReviewRepository by lazy { AppReviewRepository(dataStore) }

    @VisibleForTesting
    val measurementRepository by lazy {
        MeasurementRepository(database, json, backgroundContext)
    }

    @VisibleForTesting
    val networkRepository by lazy { NetworkRepository(database, backgroundContext) }

    @VisibleForTesting
    val preferenceRepository by lazy { PreferenceRepository(buildDataStore()) }

    @VisibleForTesting
    val resultRepository by lazy { ResultRepository(database, backgroundContext) }

    val testDescriptorRepository by lazy {
        TestDescriptorRepository(database, json, backgroundContext)
    }

    @VisibleForTesting
    val urlRepository by lazy { UrlRepository(database, backgroundContext) }

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
            getBatteryState = getBatteryState,
            platformInfo = platformInfo,
            getEnginePreferences = getEnginePreferences::invoke,
            addRunCancelListener = runBackgroundStateManager::addCancelListener,
            backgroundContext = backgroundContext,
        )
    }

    // Domain

    private val acceptDescriptorUpdate by lazy {
        AcceptDescriptorUpdate(
            saveTestDescriptors = saveTestDescriptors::invoke,
            updateState = descriptorUpdateStateManager::update,
        )
    }
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
    private val checkAutoRunConstraints by lazy {
        CheckAutoRunConstraints(
            getAutoRunSettings = getAutoRunSettings::invoke,
            getNetworkType = networkTypeFinder::invoke,
            getBatteryState = getBatteryState::invoke,
            knownNetworkType = platformInfo.knownNetworkType,
            knownBatteryState = platformInfo.knownBatteryState,
            resultRepository::countMissingUpload,
        )
    }
    private val deleteMeasurementsWithoutResult by lazy {
        DeleteMeasurementsWithoutResult(
            getMeasurementsWithoutResult = measurementRepository::listWithoutResult,
            deleteMeasurementsById = measurementRepository::deleteByIds,
            deleteFile = deleteFiles::invoke,
        )
    }
    val deleteOldResults by lazy {
        DeleteOldResults(
            getPreferenceByKey = preferenceRepository::getValueByKey,
            deleteResultsByFilter = deleteResults::byFilter,
        )
    }
    private val deleteResults by lazy {
        DeleteResults(
            deleteResultsByFilter = resultRepository::deleteByFilter,
            deleteMeasurementsWithoutResult = deleteMeasurementsWithoutResult::invoke,
            deleteNetworksWithoutResult = networkRepository::deleteWithoutResult,
            deleteAllResultsFromDatabase = resultRepository::deleteAll,
            deleteResultsByIdsFromDatabase = resultRepository::deleteByIds,
            deleteFiles = deleteFiles::invoke,
        )
    }
    private val deleteTestDescriptor by lazy {
        DeleteTestDescriptor(
            preferencesRepository = preferenceRepository,
            deleteDescriptorByRunId = testDescriptorRepository::deleteByRunId,
            deleteResultsByFilter = deleteResults::byFilter,
        )
    }
    private val descriptorUpdateStateManager by lazy { DescriptorUpdateStateManager() }
    private val dismissDescriptorReviewNotice by lazy {
        DismissDescriptorReviewNotice(
            updateState = descriptorUpdateStateManager::update,
        )
    }
    private val dismissLastRun by lazy {
        DismissLastRun(
            getLastRun = getLastRun::invoke,
            setPreference = preferenceRepository::setValueByKey,
        )
    }
    private val fetchDescriptor by lazy {
        FetchDescriptor(
            engineHttpDo = engine::httpDo,
            json = json,
        )
    }
    val finishInProgressData by lazy { FinishInProgressData(resultRepository::markAllAsDone) }
    val fetchDescriptorsUpdates by lazy {
        FetchDescriptorsUpdates(
            getLatestTestDescriptors = testDescriptorRepository::listLatest,
            fetchDescriptor = fetchDescriptor::invoke,
            saveTestDescriptors = saveTestDescriptors::invoke,
            updateState = descriptorUpdateStateManager::update,
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
    private val getEnginePreferences by lazy {
        GetEnginePreferences(
            preferencesRepository = preferenceRepository,
            getProxyOption = proxyManager::selected,
        )
    }
    private val getFirstRun by lazy { GetFirstRun(preferenceRepository) }
    private val getLastResultOfDescriptor by lazy {
        GetLastResultOfDescriptor(
            getLastResultDoneByDescriptor = resultRepository::getLastDoneByDescriptor,
            getResultById = getResult::invoke,
        )
    }
    private val getLastRun by lazy {
        GetLastRun(
            getLastResults = resultRepository::getLast,
            getPreference = preferenceRepository::getValueByKey,
        )
    }
    private val getResults by lazy {
        GetResults(
            resultRepository::list,
            getTestDescriptors::all,
            measurementRepository::selectTestKeys,
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
    val clearStorage by lazy {
        ClearStorage(
            backgroundContext = backgroundContext,
            deleteAllResults = deleteResults::all,
            clearLogs = appLogger::clear,
            updateStorageUsed = getStorageUsed::update,
            clearPreferences = preferenceRepository::clear,
        )
    }

    private val getMeasurementsNotUploaded by lazy {
        GetMeasurementsNotUploaded(
            listMeasurementsNotUploaded = measurementRepository::listNotUploaded,
            getMeasurementById = measurementRepository::getById,
        )
    }
    private val getSettings by lazy {
        GetSettings(
            preferencesRepository = preferenceRepository,
            observeStorageUsed = getStorageUsed::observe,
            clearStorage = clearStorage::invoke,
            supportsCrashReporting = flavorConfig.isCrashReportingEnabled,
            knownNetworkType = platformInfo.knownNetworkType,
            knownBatteryState = platformInfo.knownBatteryState,
            supportsInAppLanguage = platformInfo.supportsInAppLanguage,
            hasDonations = platformInfo.hasDonations,
            isCleanUpRequired = isCleanUpRequired,
            cleanupLegacyDirectories = cleanupLegacyDirectories,
        )
    }
    private val getStats by lazy {
        GetStats(
            countMeasurementsFromStartTime = measurementRepository::countFromStartTime,
            countNetworkAsns = networkRepository::countAsns,
            countNetworkCountries = networkRepository::countCountries,
        )
    }

    @VisibleForTesting
    val getTestDescriptors by lazy {
        GetTestDescriptors(
            getDefaultTestDescriptors = getDefaultTestDescriptors::invoke,
            listAllInstalledTestDescriptors = testDescriptorRepository::listAll,
            listLatestInstalledTestDescriptors = testDescriptorRepository::listLatest,
            observeDescriptorsUpdateState = descriptorUpdateStateManager::observe,
            getPreferenceValues = preferenceRepository::allSettings,
        )
    }
    private val getTestDescriptorsBySpec by lazy {
        GetTestDescriptorsBySpec(getTestDescriptors = getTestDescriptors::latest)
    }
    val markAppReviewAsShown by lazy {
        MarkAppReviewAsShown(setShownAt = appReviewRepository::setShownAt)
    }
    val observeAndConfigureAutoRun by lazy {
        ObserveAndConfigureAutoRun(
            backgroundContext = backgroundContext,
            configureAutoRun = configureAutoRun,
            getAutoRunSettings = getAutoRunSettings::invoke,
        )
    }
    val observeAndConfigureAutoUpdate by lazy {
        ObserveAndConfigureAutoUpdate(
            backgroundContext = backgroundContext,
            listAllInstalledTestDescriptors = testDescriptorRepository::listAll,
            configureDescriptorAutoUpdate = configureDescriptorAutoUpdate,
            cancelDescriptorAutoUpdate = cancelDescriptorAutoUpdate,
            startDescriptorsUpdate = startDescriptorsUpdate,
        )
    }
    private val proxyManager by lazy {
        ProxyManager(
            getPreference = preferenceRepository::getValueByKey,
            setPreference = preferenceRepository::setValueByKey,
            removePreference = preferenceRepository::remove,
            proxyConfig = proxyConfig,
        )
    }
    private val rejectDescriptorUpdate by lazy {
        RejectDescriptorUpdate(
            updateDescriptorRejectedRevision = testDescriptorRepository::updateRejectedRevision,
            updateState = descriptorUpdateStateManager::update,
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
            cancelRun = runBackgroundStateManager::cancel,
            addRunCancelListener = runBackgroundStateManager::addCancelListener,
            reportTestRunError = runBackgroundStateManager::reportError,
            getEnginePreferences = getEnginePreferences::invoke,
            finishInProgressData = finishInProgressData::invoke,
            networkTypeFinder = networkTypeFinder::invoke,
            testProxy = testProxy::invoke,
        )
    }
    private val saveTestDescriptors by lazy {
        SaveTestDescriptors(
            createOrIgnoreDescriptors = testDescriptorRepository::createOrIgnore,
            createOrUpdateDescriptors = testDescriptorRepository::createOrUpdate,
            storeUrlsByUrl = urlRepository::createOrUpdateByUrl,
        )
    }
    val sendSupportEmail by lazy {
        SendSupportEmail(
            platformInfo = platformInfo,
            launchAction = launchAction,
            getAppLoggerFile = appLogger::getLogFilePath,
        )
    }
    private val shareLogFile by lazy { ShareLogFile(launchAction, appLogger::getLogFilePath) }
    val shouldShowAppReview by lazy {
        ShouldShowAppReview(
            incrementLaunchTimes = appReviewRepository::incrementLaunchTimes,
            getLaunchTimes = appReviewRepository::getLaunchTimes,
            getShownAt = appReviewRepository::getShownAt,
            getFirstOpenAt = appReviewRepository::getFirstOpenAt,
            setFirstOpenAt = appReviewRepository::setFirstOpenAt,
        )
    }
    private val shouldShowVpnWarning by lazy {
        ShouldShowVpnWarning(preferenceRepository, networkTypeFinder::invoke)
    }
    val runBackgroundStateManager by lazy { RunBackgroundStateManager() }
    private val undoRejectedDescriptorUpdate by lazy {
        UndoRejectedDescriptorUpdate(
            updateDescriptorRejectedRevision = testDescriptorRepository::updateRejectedRevision,
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

    private val uploadMissingMeasurements by lazy {
        UploadMissingMeasurements(
            getMeasurementsNotUploaded = getMeasurementsNotUploaded::invoke,
            submitMeasurement = engine::submitMeasurements,
            readFile = readFile,
            deleteFiles = deleteFiles,
            updateMeasurement = measurementRepository::createOrUpdate,
            deleteMeasurementById = measurementRepository::deleteById,
        )
    }
    private val testProxy by lazy {
        TestProxy(
            httpDo = engine::httpDo,
            getProxyOption = proxyManager::selected,
            backgroundContext = backgroundContext,
        )
    }

    // Background

    val runBackgroundTask by lazy {
        RunBackgroundTask(
            getPreferenceValueByKey = preferenceRepository::getValueByKey,
            uploadMissingMeasurements = uploadMissingMeasurements::invoke,
            checkAutoRunConstraints = checkAutoRunConstraints::invoke,
            getAutoRunSpecification = getAutoRunSpecification::invoke,
            runDescriptors = runDescriptors::invoke,
            addRunCancelListener = runBackgroundStateManager::addCancelListener,
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

    fun addProxyViewModel(onBack: () -> Unit) =
        AddProxyViewModel(
            onBack = onBack,
            addCustomProxy = proxyManager::addCustom,
        )

    fun addDescriptorViewModel(
        descriptorId: String,
        onBack: () -> Unit,
    ) = AddDescriptorViewModel(
        onBack = onBack,
        saveTestDescriptors = saveTestDescriptors::invoke,
        fetchDescriptor = { fetchDescriptor(descriptorId) },
        preferenceRepository = preferenceRepository,
        startBackgroundRun = startSingleRunInner,
    )

    fun chooseWebsitesViewModel(
        initialUrl: String?,
        onBack: () -> Unit,
        goToDashboard: () -> Unit,
    ) = ChooseWebsitesViewModel(
        initialUrl = initialUrl,
        onBack = onBack,
        goToDashboard = goToDashboard,
        startBackgroundRun = startSingleRunInner,
        getPreference = preferenceRepository::getValueByKey,
        setPreference = preferenceRepository::setValueByKey,
    )

    fun dashboardViewModel(
        goToOnboarding: () -> Unit,
        goToResults: () -> Unit,
        goToRunningTest: () -> Unit,
        goToRunTests: () -> Unit,
        goToTests: () -> Unit,
        goToTestSettings: () -> Unit,
    ) = DashboardViewModel(
        goToOnboarding = goToOnboarding,
        goToResults = goToResults,
        goToRunningTest = goToRunningTest,
        goToRunTests = goToRunTests,
        goToTests = goToTests,
        goToTestSettings = goToTestSettings,
        getFirstRun = getFirstRun::invoke,
        observeRunBackgroundState = runBackgroundStateManager::observeState,
        observeTestRunErrors = runBackgroundStateManager::observeErrors,
        shouldShowVpnWarning = shouldShowVpnWarning::invoke,
        getAutoRunSettings = getAutoRunSettings::invoke,
        getLastRun = getLastRun::invoke,
        dismissLastRun = dismissLastRun::invoke,
        getPreference = preferenceRepository::getValueByKey,
        setPreference = preferenceRepository::setValueByKey,
        getStats = getStats::invoke,
        batteryOptimization = batteryOptimization,
    )

    fun descriptorsViewModel(
        goToDescriptor: (String) -> Unit,
        goToReviewDescriptorUpdates: (List<InstalledTestDescriptorModel.Id>?) -> Unit,
    ) = DescriptorsViewModel(
        goToDescriptor = goToDescriptor,
        goToReviewDescriptorUpdates = goToReviewDescriptorUpdates,
        getTestDescriptors = getTestDescriptors::latest,
        startDescriptorsUpdates = startDescriptorsUpdate,
        dismissDescriptorsUpdateNotice = dismissDescriptorReviewNotice::invoke,
        observeDescriptorUpdateState = descriptorUpdateStateManager::observe,
        canPullToRefresh = platformInfo.canPullToRefresh,
        getPreference = preferenceRepository::getValueByKey,
        setPreference = preferenceRepository::setValueByKey,
    )

    fun descriptorViewModel(
        descriptorKey: String,
        onBack: () -> Unit,
        goToReviewDescriptorUpdates: (List<InstalledTestDescriptorModel.Id>?) -> Unit,
        goToChooseWebsites: () -> Unit,
        goToResult: (ResultModel.Id) -> Unit,
        goToDescriptorWebsites: (InstalledTestDescriptorModel.Id) -> Unit,
    ) = DescriptorViewModel(
        descriptorKey = descriptorKey,
        onBack = onBack,
        goToReviewDescriptorUpdates = goToReviewDescriptorUpdates,
        goToChooseWebsites = goToChooseWebsites,
        goToResult = goToResult,
        goToDescriptorWebsites = goToDescriptorWebsites,
        getTestDescriptor = getTestDescriptors::single,
        getLastResultOfDescriptor = getLastResultOfDescriptor::invoke,
        preferenceRepository = preferenceRepository,
        launchAction = launchAction::invoke,
        shouldShowVpnWarning = shouldShowVpnWarning::invoke,
        deleteTestDescriptor = deleteTestDescriptor::invoke,
        startDescriptorsUpdate = startDescriptorsUpdate,
        setAutoUpdate = testDescriptorRepository::setAutoUpdate,
        observeDescriptorsUpdateState = descriptorUpdateStateManager::observe,
        dismissDescriptorReviewNotice = dismissDescriptorReviewNotice::invoke,
        undoRejectedDescriptorUpdate = undoRejectedDescriptorUpdate::invoke,
        startBackgroundRun = startSingleRunInner,
        canPullToRefresh = platformInfo.canPullToRefresh,
    )

    fun descriptorWebsitesViewModel(
        descriptorId: InstalledTestDescriptorModel.Id,
        onBack: () -> Unit,
    ) = DescriptorWebsitesViewModel(
        descriptorId = descriptorId,
        onBack = onBack,
        getTestDescriptor = getTestDescriptors::single,
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
        isCleanUpRequired = isCleanUpRequired,
        cleanupLegacyDirectories = cleanupLegacyDirectories,
    )

    fun proxyViewModel(
        onBack: () -> Unit,
        goToAddProxy: () -> Unit,
    ) = ProxyViewModel(
        onBack = onBack,
        goToAddProxy = goToAddProxy,
        getProxyOptions = proxyManager::all,
        selectProxyOption = proxyManager::select,
        deleteProxyOption = proxyManager::deleteCustom,
        testProxy = testProxy::invoke,
    )

    fun resultsViewModel(
        goToResult: (ResultModel.Id) -> Unit,
        goToUpload: () -> Unit,
    ) = ResultsViewModel(
        goToResult = goToResult,
        goToUpload = goToUpload,
        getResults = getResults::invoke,
        getDescriptors = getTestDescriptors::latest,
        getNetworks = networkRepository::list,
        deleteResultsByFilter = deleteResults::byFilter,
        deleteResults = deleteResults::byIds,
        markAsViewed = resultRepository::markAllAsViewed,
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
        getProxyOption = proxyManager::selected,
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
        goToMeasurement: (MeasurementModel.Id) -> Unit,
        goToMeasurementRaw: (MeasurementModel.Id) -> Unit,
        goToUpload: () -> Unit,
        goToDashboard: () -> Unit,
    ) = ResultViewModel(
        resultId = resultId,
        onBack = onBack,
        goToMeasurement = goToMeasurement,
        goToMeasurementRaw = goToMeasurementRaw,
        goToUpload = goToUpload,
        goToDashboard = goToDashboard,
        getResult = getResult::invoke,
        getCurrentRunBackgroundState = runBackgroundStateManager.observeState(),
        markResultAsViewed = resultRepository::markAsViewed,
        startBackgroundRun = startSingleRunInner,
    )

    fun measurementViewModel(
        measurementId: MeasurementModel.Id,
        onBack: () -> Unit,
    ) = MeasurementViewModel(
        measurementId = measurementId,
        onBack = onBack,
        getMeasurement = measurementRepository::getById,
        shareUrl = { launchAction(PlatformAction.Share(it)) },
        openUrl = { launchAction(PlatformAction.OpenUrl(it)) },
        isWebViewAvailable = isWebViewAvailable,
    )

    fun measurementRawViewModel(
        measurementId: MeasurementModel.Id,
        onBack: () -> Unit,
        goToUpload: (MeasurementModel.Id) -> Unit,
        goToMeasurement: (MeasurementModel.Id) -> Unit,
    ) = MeasurementRawViewModel(
        measurementId = measurementId,
        onBack = onBack,
        goToUpload = goToUpload,
        goToMeasurement = goToMeasurement,
        getMeasurement = measurementRepository::getById,
        readFile = readFile,
        shareFile = { launchAction(it) },
    )

    fun reviewUpdatesViewModel(
        descriptorIds: List<InstalledTestDescriptorModel.Id>?,
        onBack: () -> Unit,
    ) = ReviewUpdatesViewModel(
        ids = descriptorIds,
        onBack = onBack,
        observeDescriptorsUpdateState = descriptorUpdateStateManager::observe,
        acceptDescriptorUpdate = acceptDescriptorUpdate::invoke,
        rejectDescriptorUpdate = rejectDescriptorUpdate::invoke,
    )

    fun settingsCategoryViewModel(
        categoryKey: String,
        goToSettingsForCategory: (PreferenceCategoryKey) -> Unit,
        onBack: () -> Unit,
    ) = SettingsCategoryViewModel(
        categoryKey = categoryKey,
        onBack = onBack,
        goToSettingsForCategory = goToSettingsForCategory,
        preferenceRepository = preferenceRepository,
        getSettings = getSettings::invoke,
        batteryOptimization = batteryOptimization,
    )

    fun settingsViewModel(goToSettingsForCategory: (PreferenceCategoryKey) -> Unit) =
        SettingsViewModel(
            goToSettingsForCategory = goToSettingsForCategory,
            openAppLanguageSettings = { launchAction(PlatformAction.LanguageSettings) },
            getSettings = getSettings::invoke,
        )

    fun uploadMeasurementsViewModel(
        filter: MeasurementsFilter,
        onClose: () -> Unit,
    ) = UploadMeasurementsViewModel(
        filter = filter,
        onClose = onClose,
        uploadMissingMeasurements = uploadMissingMeasurements::invoke,
    )

    fun webCategoriesViewModel(onBack: () -> Unit) =
        WebCategoriesViewModel(
            onBack = onBack,
            getPreferencesByKeys = preferenceRepository::allSettings,
            setPreferenceValuesByKeys = preferenceRepository::setValuesByKey,
        )

    fun bottomBarViewModel() =
        BottomBarViewModel(
            countAllNotViewedFlow = resultRepository::countAllNotViewedFlow,
            runBackgroundStateFlow = runBackgroundStateManager::observeState,
            observeDescriptorUpdateState = descriptorUpdateStateManager::observe,
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
                PreferenceDataStoreFactory
                    .createWithPath(
                        produceFile = { producePath().toPath() },
                        migrations = migrations,
                    ).also { dataStore = it }
            }
    }
}
