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
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.ooni.engine.Engine
import org.ooni.engine.NetworkTypeFinder
import org.ooni.engine.OonimkallBridge
import org.ooni.engine.SecureStorage
import org.ooni.engine.TaskEventMapper
import org.ooni.passport.PassportBridge
import org.ooni.passport.PassportGet
import org.ooni.passport.PassportHttpClient
import org.ooni.passport.PassportTimeouts
import org.ooni.probe.Database
import org.ooni.probe.SharedBuildConfig
import org.ooni.probe.background.RunBackgroundTask
import org.ooni.probe.config.FlavorConfigInterface
import org.ooni.probe.config.LegacyDirectoryManager
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.config.ProxyConfig
import org.ooni.probe.data.disk.AppendFile
import org.ooni.probe.data.disk.AppendFileOkio
import org.ooni.probe.data.disk.DeleteFiles
import org.ooni.probe.data.disk.DeleteFilesOkio
import org.ooni.probe.data.disk.ReadFile
import org.ooni.probe.data.disk.ReadFileOkio
import org.ooni.probe.data.disk.WriteFile
import org.ooni.probe.data.disk.WriteFileOkio
import org.ooni.probe.data.models.ArticleModel
import org.ooni.probe.data.models.AutoRunParameters
import org.ooni.probe.data.models.BatteryState
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.PlatformAction
import org.ooni.probe.data.repositories.AppReviewRepository
import org.ooni.probe.data.repositories.ArticleRepository
import org.ooni.probe.data.repositories.MeasurementRepository
import org.ooni.probe.data.repositories.NetworkRepository
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.data.repositories.ResultRepository
import org.ooni.probe.data.repositories.TestDescriptorRepository
import org.ooni.probe.data.repositories.UrlRepository
import org.ooni.probe.domain.BootstrapPreferences
import org.ooni.probe.domain.BuildCheckInRequest
import org.ooni.probe.domain.CheckAutoRunConstraints
import org.ooni.probe.domain.CheckIn
import org.ooni.probe.domain.ClearStorage
import org.ooni.probe.domain.DeleteMeasurementsWithoutResult
import org.ooni.probe.domain.DownloadFile
import org.ooni.probe.domain.FetchGeoIpDbUpdates
import org.ooni.probe.domain.FinishInProgressData
import org.ooni.probe.domain.GetAutoRunSettings
import org.ooni.probe.domain.GetAutoRunSpecification
import org.ooni.probe.domain.GetEnginePreferences
import org.ooni.probe.domain.GetFallbackUrls
import org.ooni.probe.domain.GetFirstRun
import org.ooni.probe.domain.GetLastResultOfDescriptor
import org.ooni.probe.domain.GetMeasurementsNotUploaded
import org.ooni.probe.domain.GetRerunSpecification
import org.ooni.probe.domain.GetRunAtStartupSettings
import org.ooni.probe.domain.GetSettings
import org.ooni.probe.domain.GetStats
import org.ooni.probe.domain.GetStorageUsed
import org.ooni.probe.domain.ObserveAndConfigureAutoRun
import org.ooni.probe.domain.ObserveAndConfigureAutoUpdate
import org.ooni.probe.domain.ObserveAndConfigureRunAtStartup
import org.ooni.probe.domain.RunBackgroundStateManager
import org.ooni.probe.domain.RunDescriptors
import org.ooni.probe.domain.RunNetTest
import org.ooni.probe.domain.SendSupportEmail
import org.ooni.probe.domain.ShareLogFile
import org.ooni.probe.domain.ShouldShowVpnWarning
import org.ooni.probe.domain.SubmitMeasurement
import org.ooni.probe.domain.UpdateRequiredStateManager
import org.ooni.probe.domain.UploadMissingMeasurements
import org.ooni.probe.domain.appreview.MarkAppReviewAsShown
import org.ooni.probe.domain.appreview.ShouldShowAppReview
import org.ooni.probe.domain.articles.ArticlesRefreshStateManager
import org.ooni.probe.domain.articles.GetFindings
import org.ooni.probe.domain.articles.GetRSSFeed
import org.ooni.probe.domain.articles.RefreshArticles
import org.ooni.probe.domain.credentials.ClearCredential
import org.ooni.probe.domain.credentials.GetAnonymousCredentialsHealth
import org.ooni.probe.domain.credentials.GetCredential
import org.ooni.probe.domain.credentials.GetManifest
import org.ooni.probe.domain.credentials.HandleSubmitOutcome
import org.ooni.probe.domain.credentials.PrepareAnonymousCredentials
import org.ooni.probe.domain.credentials.RegisterUser
import org.ooni.probe.domain.credentials.ResolveSubmissionPolicy
import org.ooni.probe.domain.credentials.RetrieveManifest
import org.ooni.probe.domain.credentials.SetCredential
import org.ooni.probe.domain.credentials.StampMeasurement
import org.ooni.probe.domain.credentials.SubmitMeasurementWithUser
import org.ooni.probe.domain.descriptors.AcceptDescriptorUpdate
import org.ooni.probe.domain.descriptors.BootstrapTestDescriptors
import org.ooni.probe.domain.descriptors.DeleteTestDescriptor
import org.ooni.probe.domain.descriptors.DescriptorUpdateStateManager
import org.ooni.probe.domain.descriptors.DismissDescriptorReviewNotice
import org.ooni.probe.domain.descriptors.FetchDescriptor
import org.ooni.probe.domain.descriptors.FetchDescriptorsUpdates
import org.ooni.probe.domain.descriptors.GetBootstrapTestDescriptors
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
import org.ooni.probe.locale.LocaleController
import org.ooni.probe.shared.ConnectivityMonitor
import org.ooni.probe.shared.PlatformInfo
import org.ooni.probe.shared.monitoring.AppLogger
import org.ooni.probe.shared.monitoring.CrashMonitoring
import kotlin.coroutines.CoroutineContext

/**
 * Engine, passport, and domain wiring shared by every app shell (Android/iOS/desktop UI and a
 * future desktop CLI). Holds no Compose Multiplatform types - those live on [ComposeDependencies].
 */
open class CoreDependencies(
    val platformInfo: PlatformInfo,
    private val oonimkallBridge: OonimkallBridge,
    val passportBridge: PassportBridge,
    private val baseFileDir: String,
    val cacheDir: String,
    private val databaseDriverFactory: () -> SqlDriver,
    private val networkTypeFinder: NetworkTypeFinder,
    val secureStorage: SecureStorage,
    @get:VisibleForTesting
    val buildDataStore: () -> DataStore<Preferences>,
    private val getBatteryState: () -> BatteryState,
    private val configureAutoRun: suspend (AutoRunParameters) -> Unit,
    val configureDescriptorAutoUpdate: suspend () -> Boolean,
    val cancelDescriptorAutoUpdate: suspend () -> Boolean,
    val startDescriptorsUpdate: suspend (List<Descriptor>?) -> Unit,
    private val setRunAtStartup: suspend (Boolean) -> Unit = {},
    val launchAction: (PlatformAction) -> Boolean,
    val legacyDirectoryManager: LegacyDirectoryManager = object : LegacyDirectoryManager {},
    val flavorConfig: FlavorConfigInterface,
    val proxyConfig: ProxyConfig,
    val getCountryNameByCode: (String) -> String,
    @get:VisibleForTesting
    var databaseContext: CoroutineContext = Dispatchers.IO,
) {
    // Common

    @VisibleForTesting
    var backgroundContext: CoroutineContext = Dispatchers.IO

    // Data

    val json by lazy { buildJson() }
    private val database by lazy { buildDatabase(databaseDriverFactory) }

    private val appReviewRepository by lazy { AppReviewRepository(dataStore) }

    @VisibleForTesting
    val articleRepository by lazy { ArticleRepository(database, databaseContext) }

    @VisibleForTesting
    val measurementRepository by lazy {
        MeasurementRepository(database, json, databaseContext)
    }

    @VisibleForTesting
    val networkRepository by lazy { NetworkRepository(database, databaseContext) }

    @VisibleForTesting
    val preferenceRepository by lazy { PreferenceRepository(buildDataStore()) }

    val localeController by lazy {
        LocaleController(
            getValueByKey = preferenceRepository::getValueByKey,
            languageSupport = platformInfo.languageSupport,
        )
    }

    @VisibleForTesting
    val resultRepository by lazy { ResultRepository(database, databaseContext) }

    val testDescriptorRepository by lazy {
        TestDescriptorRepository(database, json, databaseContext)
    }

    @VisibleForTesting
    val urlRepository by lazy { UrlRepository(database, databaseContext) }

    val readFile: ReadFile by lazy { ReadFileOkio(FileSystem.SYSTEM, baseFileDir) }
    private val writeFile: WriteFile by lazy { WriteFileOkio(FileSystem.SYSTEM, baseFileDir) }
    private val appendFile: AppendFile by lazy { AppendFileOkio(FileSystem.SYSTEM, baseFileDir) }
    private val deleteFiles: DeleteFiles by lazy {
        DeleteFilesOkio(
            fileSystem = FileSystem.SYSTEM,
            baseFilesDir = baseFileDir,
            backgroundContext = backgroundContext,
        )
    }

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
            appendFile = appendFile,
            deleteFiles = deleteFiles,
            backgroundContext = backgroundContext,
        )
    }

    // Connectivity

    val connectivityMonitor by lazy { ConnectivityMonitor(networkTypeFinder) }

    // Engine
    private val taskEventMapper by lazy { TaskEventMapper(networkTypeFinder, json) }

    private val downloader by lazy {
        DownloadFile(
            fileSystem = FileSystem.SYSTEM,
            isOnline = connectivityMonitor::isOnline,
        )
    }

    val fetchGeoIpDbUpdates by lazy {
        FetchGeoIpDbUpdates(
            downloadFile = downloader::invoke,
            cacheDir = cacheDir,
            // Opportunistic: nothing in the UI waits for a GeoIP release lookup.
            passportGet = { url ->
                passportHttpClient.get(url, timeout = PassportTimeouts.PREFETCH_SECONDS)
            },
            json = json,
            preferencesRepository = preferenceRepository,
            fileSystem = FileSystem.SYSTEM,
            backgroundContext = backgroundContext,
        )
    }

    private val coreConfig by lazy {
        org.ooni.probe.config.CoreConfig(
            baseSoftwareName = OrganizationConfig.baseSoftwareName,
            ooniApiBaseUrl = OrganizationConfig.ooniApiBaseUrl,
            passportVersion = SharedBuildConfig.PASSPORT_VERSION,
        )
    }

    @VisibleForTesting
    val engine by lazy {
        Engine(
            bridge = oonimkallBridge,
            json = json,
            baseFilePath = baseFileDir,
            cacheDir = cacheDir,
            taskEventMapper = taskEventMapper,
            networkTypeFinder = networkTypeFinder,
            platformInfo = platformInfo,
            coreConfig = coreConfig,
            getEnginePreferences = getEnginePreferences::invoke,
            addRunCancelListener = runBackgroundStateManager::addCancelListener,
            backgroundContext = backgroundContext,
        )
    }

    // Domain

    val acceptDescriptorUpdate by lazy {
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
    val buildCheckInRequest by lazy {
        BuildCheckInRequest(
            getEnginePreferences = getEnginePreferences::invoke,
            platformInfo = platformInfo,
            getBatteryState = getBatteryState::invoke,
            networkTypeFinder = networkTypeFinder,
            coreConfig = coreConfig,
        )
    }
    val cancelCurrentTest get() = runBackgroundStateManager::cancel
    private val checkIn by lazy {
        CheckIn(
            // The user is waiting on a run start, and check-in decides which URLs get measured,
            // so it gets the full timeout. Failing (including offline) falls back to local URLs
            // in RunDescriptors and never prevents the tests themselves from running.
            passportPost = { url, payload ->
                passportHttpClient.post(url, payload, timeout = PassportTimeouts.DEFAULT_SECONDS)
            },
            storeUrlsByUrl = urlRepository::createOrUpdateByUrl,
            buildCheckInRequest = buildCheckInRequest::invoke,
            json = json,
            setPreferenceByKey = preferenceRepository::setValueByKey,
            ooniApiBaseUrl = OrganizationConfig.ooniApiBaseUrl,
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
    val deleteResults by lazy {
        DeleteResults(
            deleteResultsByFilter = resultRepository::deleteByFilter,
            deleteMeasurementsWithoutResult = deleteMeasurementsWithoutResult::invoke,
            deleteNetworksWithoutResult = networkRepository::deleteWithoutResult,
            deleteAllResultsFromDatabase = resultRepository::deleteAll,
            deleteResultsByIdsFromDatabase = resultRepository::deleteByIds,
            deleteFiles = deleteFiles::invoke,
        )
    }
    val deleteTestDescriptor by lazy {
        DeleteTestDescriptor(
            preferencesRepository = preferenceRepository,
            deleteDescriptorByRunId = testDescriptorRepository::deleteByRunId,
            deleteResultsByFilter = deleteResults::byFilter,
        )
    }
    val descriptorUpdateStateManager by lazy { DescriptorUpdateStateManager() }
    val dismissDescriptorReviewNotice by lazy {
        DismissDescriptorReviewNotice(
            updateState = descriptorUpdateStateManager::update,
        )
    }
    val dismissLastRun by lazy {
        DismissLastRun(
            getLastRun = getLastRun::invoke,
            setPreference = preferenceRepository::setValueByKey,
        )
    }
    val fetchDescriptor by lazy {
        FetchDescriptor(
            // Shared by the background update worker and manual "add descriptor". The background
            // case dominates and must not stall, and the manual screen already shows a loading
            // state, so the shorter timeout is the right default for both.
            passportGet = { url ->
                passportHttpClient.get(url, timeout = PassportTimeouts.PREFETCH_SECONDS)
            },
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
        GetBootstrapTestDescriptors(json, backgroundContext)
    }
    private val getEnginePreferences by lazy {
        GetEnginePreferences(
            preferencesRepository = preferenceRepository,
            getProxyOption = proxyManager::selected,
            cacheDir = cacheDir,
        )
    }
    val getFirstRun by lazy { GetFirstRun(preferenceRepository) }
    val getLastResultOfDescriptor by lazy {
        GetLastResultOfDescriptor(
            getLastResultDoneByDescriptor = resultRepository::getLastDoneByDescriptor,
            getResultById = getResult::invoke,
        )
    }
    val getLastRun by lazy {
        GetLastRun(
            getLastRunResults = resultRepository::getLastRunResults,
            getLastResult = resultRepository::getLatest,
            getPreference = preferenceRepository::getValueByKey,
        )
    }
    private val getRerunSpecification by lazy {
        GetRerunSpecification(getResult::invoke)
    }
    private val getManifest by lazy {
        GetManifest(
            getPreference = preferenceRepository::getValueByKey,
            json = json,
        )
    }
    private val getCredential by lazy {
        GetCredential(
            readSecureStorage = secureStorage::read,
            json = json,
        )
    }
    private val registerUser by lazy {
        RegisterUser(
            // Registration blocks credential availability: full timeout.
            userAuthRegister = { url, publicParams, manifestVersion ->
                passportHttpClient.userAuthRegister(
                    url = url,
                    publicParams = publicParams,
                    manifestVersion = manifestVersion,
                    timeout = PassportTimeouts.DEFAULT_SECONDS,
                )
            },
            setCredential = setCredential::invoke,
            backgroundContext = backgroundContext,
            json = json,
            ooniApiBaseUrl = OrganizationConfig.ooniApiBaseUrl,
        )
    }
    val prepareAnonymousCredentials by lazy {
        PrepareAnonymousCredentials(
            getManifest = getManifest::invoke,
            retrieveManifest = retrieveManifest::invoke,
            getCredential = getCredential::invoke,
            registerUser = registerUser::invoke,
            backgroundContext = backgroundContext,
        )
    }
    private val getFallbackUrls by lazy {
        GetFallbackUrls(
            getMeasurementsWithUrl = measurementRepository::listWithUrl,
            getBatteryState = getBatteryState,
        )
    }
    val getResults by lazy {
        GetResults(
            resultRepository::list,
            getTestDescriptors::all,
            measurementRepository::selectTestKeys,
        )
    }
    val getResult by lazy {
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
    val getSettings by lazy {
        GetSettings(
            preferencesRepository = preferenceRepository,
            observeStorageUsed = getStorageUsed::observe,
            clearStorage = clearStorage::invoke,
            supportsCrashReporting = flavorConfig.isCrashReportingEnabled,
            knownNetworkType = platformInfo.knownNetworkType,
            knownBatteryState = platformInfo.knownBatteryState,
            languageSupport = platformInfo.languageSupport,
            supportsRunAtStartup = platformInfo.supportsRunAtStartup,
            hasDonations = platformInfo.hasDonations,
            isCleanUpRequired = legacyDirectoryManager::isCleanUpRequired,
            cleanupLegacyDirectories = legacyDirectoryManager::cleanUp,
        )
    }
    val getStats by lazy {
        GetStats(
            countMeasurementsFromStartTime = measurementRepository::countFromStartTime,
            countNetworkAsns = networkRepository::countAsns,
            getNetworkCountries = networkRepository::listCountries,
            getCountryNameByCode = getCountryNameByCode,
        )
    }

    @VisibleForTesting
    val getTestDescriptors by lazy {
        GetTestDescriptors(
            listAllInstalledTestDescriptors = testDescriptorRepository::listAll,
            listLatestInstalledTestDescriptors = testDescriptorRepository::listLatest,
            observeDescriptorsUpdateState = descriptorUpdateStateManager::observe,
            getPreferenceValues = preferenceRepository::allSettings,
            getPreferenceByKey = preferenceRepository::getValueByKey,
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
    private val getRunAtStartupSettings by lazy {
        GetRunAtStartupSettings(preferenceRepository::getValueByKey)
    }
    val observeAndConfigureRunAtStartup by lazy {
        ObserveAndConfigureRunAtStartup(
            backgroundContext = backgroundContext,
            getRunAtStartupSettings = getRunAtStartupSettings::invoke,
            getAutoRunSettings = getAutoRunSettings::invoke,
            setPreference = preferenceRepository::setValueByKey,
            configureRunAtStartup = setRunAtStartup,
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
    val proxyManager by lazy {
        ProxyManager(
            getPreference = preferenceRepository::getValueByKey,
            setPreference = preferenceRepository::setValueByKey,
            removePreference = preferenceRepository::remove,
            proxyConfig = proxyConfig,
        )
    }

    @get:VisibleForTesting
    val passportHttpClient by lazy {
        PassportHttpClient(
            passportGet = passportBridge::get,
            passportPost = passportBridge::post,
            passportAuthRegister = passportBridge::userAuthRegister,
            passportAuthSubmit = passportBridge::userAuthSubmit,
            getProxyOption = proxyManager::selected,
            backgroundContext = backgroundContext,
            isOnline = connectivityMonitor::isOnline,
        )
    }

    @VisibleForTesting
    var passportGet: PassportGet
        get() = passportHttpClient.passportGet
        set(value) {
            passportHttpClient.passportGet = value
        }

    val rejectDescriptorUpdate by lazy {
        RejectDescriptorUpdate(
            updateDescriptorRejectedRevision = testDescriptorRepository::updateRejectedRevision,
            updateState = descriptorUpdateStateManager::update,
        )
    }
    private val retrieveManifest by lazy {
        RetrieveManifest(
            // Credential path: registration cannot proceed without it, so use the full timeout.
            passportGet = { url ->
                passportHttpClient.get(url, timeout = PassportTimeouts.DEFAULT_SECONDS)
            },
            setPreference = preferenceRepository::setValueByKey,
            json = json,
            backgroundContext = backgroundContext,
            ooniApiBaseUrl = OrganizationConfig.ooniApiBaseUrl,
        )
    }
    private val runDescriptors by lazy {
        RunDescriptors(
            getTestDescriptorsBySpec = getTestDescriptorsBySpec::invoke,
            checkIn = checkIn::invoke,
            getFallbackUrls = getFallbackUrls::invoke,
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
    val saveTestDescriptors by lazy {
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
    val setCredential by lazy {
        SetCredential(
            writeSecureStorage = secureStorage::write,
            json = json,
        )
    }
    val shareLogFile by lazy { ShareLogFile(launchAction, appLogger::getLogFilePath) }
    val shouldShowAppReview by lazy {
        ShouldShowAppReview(
            incrementLaunchTimes = appReviewRepository::incrementLaunchTimes,
            getLaunchTimes = appReviewRepository::getLaunchTimes,
            getShownAt = appReviewRepository::getShownAt,
            getFirstOpenAt = appReviewRepository::getFirstOpenAt,
            setFirstOpenAt = appReviewRepository::setFirstOpenAt,
        )
    }
    val shouldShowVpnWarning by lazy {
        ShouldShowVpnWarning(preferenceRepository, networkTypeFinder::invoke)
    }
    val refreshArticles by lazy {
        RefreshArticles(
            hasOoniNews = OrganizationConfig.hasOoniNews,
            // Articles are opportunistic content: a slow feed must never hold up app start.
            sources = listOf(
                GetRSSFeed(
                    passportGet = { url -> passportHttpClient.get(url, timeout = ARTICLES_TIMEOUT) },
                    "https://ooni.org/blog/index.xml",
                    ArticleModel.Source.Blog,
                ),
                GetRSSFeed(
                    passportGet = { url -> passportHttpClient.get(url, timeout = ARTICLES_TIMEOUT) },
                    "https://ooni.org/reports/index.xml",
                    ArticleModel.Source.Report,
                ),
                GetFindings(
                    passportGet = { url -> passportHttpClient.get(url, timeout = ARTICLES_TIMEOUT) },
                    json,
                ),
            ),
            isOnline = connectivityMonitor::isOnline,
            refreshArticlesInDatabase = articleRepository::refresh,
            getPreference = preferenceRepository::getValueByKey,
            setPreference = preferenceRepository::setValueByKey,
            updateState = articlesRefreshStateManager::update,
        )
    }
    val articlesRefreshStateManager by lazy { ArticlesRefreshStateManager() }
    val runBackgroundStateManager by lazy { RunBackgroundStateManager() }
    val updateRequiredStateManager by lazy { UpdateRequiredStateManager() }
    val undoRejectedDescriptorUpdate by lazy {
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
            getPreferenceValueByKey = preferenceRepository::getValueByKey,
            submitMeasurement = submitMeasurement::invoke,
            spec = spec,
        )

    private val submitMeasurement by lazy {
        SubmitMeasurement(
            submitMeasurementWithUser = submitMeasurementWithUser::invoke,
            engineSubmit = engine::submitMeasurement,
            readFile = readFile,
            deleteFiles = deleteFiles,
            updateMeasurement = measurementRepository::createOrUpdate,
            deleteMeasurementById = measurementRepository::deleteById,
            handleSubmitOutcome = handleSubmitOutcome::invoke,
            json = json,
        )
    }
    val clearCredential by lazy {
        ClearCredential(deleteSecureStorage = secureStorage::delete)
    }
    val getAnonymousCredentialsHealth by lazy {
        GetAnonymousCredentialsHealth(
            getCredential = getCredential::invoke,
            getLatestNetwork = networkRepository::latest,
            passportGetProbeId = passportBridge::getProbeId,
        )
    }
    private val handleSubmitOutcome by lazy {
        HandleSubmitOutcome(
            retrieveManifest = { retrieveManifest() },
            clearCredential = clearCredential,
            signalUpdateRequired = updateRequiredStateManager::signalUpdateRequired,
        )
    }
    private val submitMeasurementWithUser by lazy {
        SubmitMeasurementWithUser(
            getManifest = getManifest::invoke,
            getCredential = getCredential::invoke,
            setCredential = setCredential,
            stampMeasurement = stampMeasurement,
            resolveSubmissionPolicy = resolveSubmissionPolicy,
            // Uploading a measurement the user already produced: full timeout, and offline is
            // handled by the gate returning Offline so nothing is lost - the measurement stays
            // not-uploaded and UploadMissingMeasurements retries it later.
            userAuthSubmit = { url, content, probeCc, probeAsn, credentialConfig ->
                passportHttpClient.userAuthSubmit(
                    url = url,
                    content = content,
                    probeCc = probeCc,
                    probeAsn = probeAsn,
                    credentialConfig = credentialConfig,
                    timeout = PassportTimeouts.DEFAULT_SECONDS,
                )
            },
            json = json,
            ooniApiBaseUrl = OrganizationConfig.ooniApiBaseUrl,
        )
    }
    private val resolveSubmissionPolicy by lazy { ResolveSubmissionPolicy() }
    private val stampMeasurement by lazy {
        StampMeasurement(
            passportGetProbeId = passportBridge::getProbeId,
            getCredential = getCredential::invoke,
            json = json,
        )
    }
    val uploadMissingMeasurements by lazy {
        UploadMissingMeasurements(
            getMeasurementsNotUploaded = getMeasurementsNotUploaded::invoke,
            submitMeasurement = submitMeasurement::invoke,
        )
    }
    val testProxy by lazy {
        TestProxy(
            getProxyOption = proxyManager::selected,
            // Health check runs alongside a test run; it must not outlive its usefulness.
            passportGet = { url, proxy ->
                passportHttpClient.get(url, proxy, timeout = PassportTimeouts.PREFETCH_SECONDS)
            },
            backgroundContext = backgroundContext,
            ooniApiBaseUrl = OrganizationConfig.ooniApiBaseUrl,
        )
    }

    // Background

    val runBackgroundTask by lazy {
        RunBackgroundTask(
            getPreferenceValueByKey = preferenceRepository::getValueByKey,
            prepareAnonymousCredentials = prepareAnonymousCredentials::invoke,
            uploadMissingMeasurements = uploadMissingMeasurements::invoke,
            checkAutoRunConstraints = checkAutoRunConstraints::invoke,
            getAutoRunSpecification = getAutoRunSpecification::invoke,
            getRerunSpecification = getRerunSpecification::invoke,
            runDescriptors = runDescriptors::invoke,
            addRunCancelListener = runBackgroundStateManager::addCancelListener,
            setRunBackgroundState = runBackgroundStateManager::updateState,
            getRunBackgroundState = runBackgroundStateManager::observeState,
        )
    }

    companion object {
        private const val ARTICLES_TIMEOUT = PassportTimeouts.PREFETCH_SECONDS

        fun buildJson() =
            Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
                isLenient = true
            }

        @VisibleForTesting
        fun buildDatabase(driverFactory: () -> SqlDriver): Database = Database(driverFactory())

        private lateinit var dataStore: DataStore<Preferences>
        const val DATA_STORE_FILE_NAME = "probe.preferences_pb"

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
