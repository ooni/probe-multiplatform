package org.ooni.probe.di

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.unit.LayoutDirection
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.ooni.engine.NetworkTypeFinder
import org.ooni.engine.OonimkallBridge
import org.ooni.engine.SecureStorage
import org.ooni.passport.PassportBridge
import org.ooni.probe.SharedBuildConfig
import org.ooni.probe.config.BatteryOptimization
import org.ooni.probe.config.FlavorConfigInterface
import org.ooni.probe.config.LegacyDirectoryManager
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.config.ProxyConfig
import org.ooni.probe.data.models.ArticleModel
import org.ooni.probe.data.models.AutoRunParameters
import org.ooni.probe.data.models.BatteryState
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementsFilter
import org.ooni.probe.data.models.PlatformAction
import org.ooni.probe.data.models.PreferenceCategoryKey
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.shared.PlatformInfo
import org.ooni.probe.shared.monitoring.Instrumentation
import org.ooni.probe.shared.monitoring.createInstrumentationDelegate
import org.ooni.probe.ui.articles.ArticleViewModel
import org.ooni.probe.ui.articles.ArticlesViewModel
import org.ooni.probe.ui.choosewebsites.ChooseWebsitesViewModel
import org.ooni.probe.ui.dashboard.DashboardViewModel
import org.ooni.probe.ui.descriptor.DescriptorViewModel
import org.ooni.probe.ui.descriptor.add.AddDescriptorUrlViewModel
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
import org.ooni.probe.ui.settings.credentials.AnonymousCredentialsViewModel
import org.ooni.probe.ui.settings.language.LanguageViewModel
import org.ooni.probe.ui.settings.proxy.AddProxyViewModel
import org.ooni.probe.ui.settings.proxy.ProxyViewModel
import org.ooni.probe.ui.settings.webcategories.WebCategoriesViewModel
import org.ooni.probe.ui.upload.UploadMeasurementsViewModel
import kotlin.coroutines.CoroutineContext

/**
 * Adds the Compose Multiplatform UI wiring (ViewModel factories, locale/webview/language
 * plumbing) on top of [CoreDependencies]. This is the class every Compose app shell
 * (Android/iOS/desktop) constructs; a future desktop CLI would construct [CoreDependencies]
 * directly instead.
 */
class ComposeDependencies(
    platformInfo: PlatformInfo,
    oonimkallBridge: OonimkallBridge,
    passportBridge: PassportBridge,
    baseFileDir: String,
    cacheDir: String,
    databaseDriverFactory: () -> SqlDriver,
    networkTypeFinder: NetworkTypeFinder,
    secureStorage: SecureStorage,
    buildDataStore: () -> DataStore<Preferences>,
    getBatteryState: () -> BatteryState,
    val startSingleRunInner: (RunSpecification) -> Unit,
    configureAutoRun: suspend (AutoRunParameters) -> Unit,
    configureDescriptorAutoUpdate: suspend () -> Boolean,
    cancelDescriptorAutoUpdate: suspend () -> Boolean,
    startDescriptorsUpdate: suspend (List<Descriptor>?) -> Unit,
    setRunAtStartup: suspend (Boolean) -> Unit = {},
    val localeDirection: (() -> LayoutDirection)? = null,
    private val isWebViewAvailable: () -> Boolean,
    launchAction: (PlatformAction) -> Boolean,
    legacyDirectoryManager: LegacyDirectoryManager = object : LegacyDirectoryManager {},
    @get:VisibleForTesting
    val batteryOptimization: BatteryOptimization,
    flavorConfig: FlavorConfigInterface,
    proxyConfig: ProxyConfig,
    getCountryNameByCode: (String) -> String,
    val getLanguageNameByCode: (String) -> String = { it },
    val supportedLanguageTags: List<String> = SharedBuildConfig.SUPPORTED_LANGUAGES,
    @get:VisibleForTesting
    databaseContext: CoroutineContext = Dispatchers.IO,
) : CoreDependencies(
        platformInfo = platformInfo,
        oonimkallBridge = oonimkallBridge,
        passportBridge = passportBridge,
        baseFileDir = baseFileDir,
        cacheDir = cacheDir,
        databaseDriverFactory = databaseDriverFactory,
        networkTypeFinder = networkTypeFinder,
        secureStorage = secureStorage,
        buildDataStore = buildDataStore,
        getBatteryState = getBatteryState,
        configureAutoRun = configureAutoRun,
        configureDescriptorAutoUpdate = configureDescriptorAutoUpdate,
        cancelDescriptorAutoUpdate = cancelDescriptorAutoUpdate,
        startDescriptorsUpdate = startDescriptorsUpdate,
        setRunAtStartup = setRunAtStartup,
        launchAction = launchAction,
        legacyDirectoryManager = legacyDirectoryManager,
        flavorConfig = flavorConfig,
        proxyConfig = proxyConfig,
        getCountryNameByCode = getCountryNameByCode,
        databaseContext = databaseContext,
    ) {
    init {
        // Install the platform's real (Sentry-backed) instrumentation delegate into probeCore's
        // no-op Instrumentation. A CLI constructing only CoreDependencies leaves the no-op default
        // in place.
        Instrumentation.delegate = createInstrumentationDelegate()
    }

    // ViewModels

    fun launchUpdateAction(): Boolean = OrganizationConfig.installUrl?.let { launchAction(PlatformAction.OpenUrl(it)) } ?: false

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
        onBack: () -> Unit,
        goToDescriptor: (Descriptor.Id) -> Unit,
        descriptorId: Descriptor.Id,
    ) = AddDescriptorViewModel(
        onBack = onBack,
        goToDescriptor = goToDescriptor,
        descriptorId = descriptorId,
        listDescriptorsByIds = testDescriptorRepository::listLatestByIds,
        fetchDescriptor = fetchDescriptor::invoke,
        saveTestDescriptors = saveTestDescriptors::invoke,
        preferenceRepository = preferenceRepository,
        startBackgroundRun = startSingleRunInner,
    )

    fun addDescriptorUrlViewModel(
        onClose: () -> Unit,
        goToAddDescriptor: (Descriptor.Id) -> Unit,
    ) = AddDescriptorUrlViewModel(
        onClose = onClose,
        goToAddDescriptor = goToAddDescriptor,
    )

    fun articleViewModel(
        url: ArticleModel.Url,
        onBack: () -> Unit,
    ) = ArticleViewModel(
        url = url,
        onBack = onBack,
        launchAction = launchAction::invoke,
        isWebViewAvailable = isWebViewAvailable,
    )

    fun articlesViewModel(
        onBack: () -> Unit,
        goToArticle: (ArticleModel.Url) -> Unit,
    ) = ArticlesViewModel(
        onBack = onBack,
        goToArticle = goToArticle,
        getArticles = articleRepository::list,
        refreshArticles = refreshArticles::invoke,
        canPullToRefresh = platformInfo.canPullToRefresh,
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
        goToResults: () -> Unit,
        goToRunningTest: () -> Unit,
        goToRunTests: () -> Unit,
        goToTests: () -> Unit,
        goToTestSettings: () -> Unit,
        goToArticles: () -> Unit,
        goToArticle: (ArticleModel.Url) -> Unit,
    ) = DashboardViewModel(
        goToResults = goToResults,
        goToRunningTest = goToRunningTest,
        goToRunTests = goToRunTests,
        goToTests = goToTests,
        goToTestSettings = goToTestSettings,
        goToArticles = goToArticles,
        goToArticle = goToArticle,
        observeRunBackgroundState = runBackgroundStateManager::observeState,
        observeTestRunErrors = runBackgroundStateManager::observeErrors,
        observeUpdateRequired = updateRequiredStateManager::observeUpdateRequired,
        onUpdateClicked = ::launchUpdateAction,
        dismissUpdateRequired = updateRequiredStateManager::dismiss,
        shouldShowVpnWarning = shouldShowVpnWarning::invoke,
        getAutoRunSettings = getAutoRunSettings::invoke,
        getLastRun = getLastRun::invoke,
        dismissLastRun = dismissLastRun::invoke,
        getPreference = preferenceRepository::getValueByKey,
        setPreference = preferenceRepository::setValueByKey,
        getStats = getStats::invoke,
        getArticles = articleRepository::list,
        batteryOptimization = batteryOptimization,
    )

    fun descriptorsViewModel(
        goToDescriptor: (Descriptor.Id) -> Unit,
        goToReviewDescriptorUpdates: (List<Descriptor.Id>?) -> Unit,
        goToAddDescriptorUrl: () -> Unit,
    ) = DescriptorsViewModel(
        goToDescriptor = goToDescriptor,
        goToReviewDescriptorUpdates = goToReviewDescriptorUpdates,
        goToAddDescriptorUrl = goToAddDescriptorUrl,
        getTestDescriptors = getTestDescriptors::latest,
        startDescriptorsUpdates = startDescriptorsUpdate,
        dismissDescriptorsUpdateNotice = dismissDescriptorReviewNotice::invoke,
        observeDescriptorUpdateState = descriptorUpdateStateManager::observe,
        canPullToRefresh = platformInfo.canPullToRefresh,
        getPreference = preferenceRepository::getValueByKey,
        setPreference = preferenceRepository::setValueByKey,
    )

    fun descriptorViewModel(
        descriptorId: Descriptor.Id,
        onBack: () -> Unit,
        goToReviewDescriptorUpdates: (List<Descriptor.Id>?) -> Unit,
        goToChooseWebsites: () -> Unit,
        goToResult: (ResultModel.Id) -> Unit,
        goToDescriptorWebsites: (Descriptor.Id) -> Unit,
    ) = DescriptorViewModel(
        descriptorId = descriptorId,
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
        descriptorId: Descriptor.Id,
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
        isCleanUpRequired = legacyDirectoryManager::isCleanUpRequired,
        cleanupLegacyDirectories = legacyDirectoryManager::cleanUp,
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
        getResultsStats = resultRepository::countByFilter,
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
        descriptorIds: List<Descriptor.Id>?,
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
            languageSupport = platformInfo.languageSupport,
        )

    fun anonymousCredentialsViewModel(onBack: () -> Unit) =
        AnonymousCredentialsViewModel(
            onBack = onBack,
            getHealth = getAnonymousCredentialsHealth::invoke,
            clearCredential = clearCredential::invoke,
            registerCredential = { prepareAnonymousCredentials() != null },
        )

    fun languageViewModel(onBack: () -> Unit) =
        LanguageViewModel(
            onBack = onBack,
            supportedLanguages = supportedLanguageTags,
            getLanguageName = getLanguageNameByCode,
            getPreference = preferenceRepository::getValueByKey,
            setPreference = preferenceRepository::setValueByKey,
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
}
