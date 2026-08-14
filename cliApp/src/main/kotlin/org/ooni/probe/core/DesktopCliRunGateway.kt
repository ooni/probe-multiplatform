package org.ooni.probe.core

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okio.FileSystem
import org.ooni.engine.DesktopNetworkTypeFinder
import org.ooni.engine.DesktopOonimkallBridge
import org.ooni.engine.Engine
import org.ooni.engine.TaskEventMapper
import org.ooni.engine.createDesktopSecureStorage
import org.ooni.engine.models.EnginePreferences
import org.ooni.engine.models.Failure
import org.ooni.engine.models.TaskLogLevel
import org.ooni.passport.CliDesktopPassportBridge
import org.ooni.passport.PassportBridge
import org.ooni.passport.PassportHttpClient
import org.ooni.probe.Database
import org.ooni.probe.config.CoreConfig
import org.ooni.probe.data.buildDatabaseDriver
import org.ooni.probe.data.disk.DeleteFilesOkio
import org.ooni.probe.data.disk.ReadFileOkio
import org.ooni.probe.data.disk.WriteFileOkio
import org.ooni.probe.data.models.BatteryState
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.ProxyOption
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.models.toDescriptorItem
import org.ooni.probe.data.repositories.MeasurementRepository
import org.ooni.probe.data.repositories.NetworkRepository
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.data.repositories.ResultRepository
import org.ooni.probe.data.repositories.TestDescriptorRepository
import org.ooni.probe.data.repositories.UrlRepository
import org.ooni.probe.domain.BuildCheckInRequest
import org.ooni.probe.domain.CancelListenerCallback
import org.ooni.probe.domain.CheckIn
import org.ooni.probe.domain.FinishInProgressData
import org.ooni.probe.domain.GetFallbackUrls
import org.ooni.probe.domain.GetMeasurementsNotUploaded
import org.ooni.probe.domain.RunBackgroundStateManager
import org.ooni.probe.domain.RunDescriptors
import org.ooni.probe.domain.RunNetTest
import org.ooni.probe.domain.SubmitMeasurement
import org.ooni.probe.domain.UploadMissingMeasurements
import org.ooni.probe.domain.credentials.ClearCredential
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
import org.ooni.probe.domain.descriptors.GetTestDescriptorsBySpec
import org.ooni.probe.domain.proxy.TestProxy
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo
import java.io.File

/**
 * Builds the desktop/JVM CLI run gateway.
 *
 * Mirrors [buildDesktopCliUploadGateway] for the engine/repositories/submit/upload wiring and adds
 * the canonical run chain: check-in, bootstrap descriptors, [RunNetTest], [RunDescriptors],
 * [org.ooni.probe.background.RunBackgroundTask] (via [CliRunOrchestrator]), plus the credential
 * chain for the with-credentials path.
 */
fun buildDesktopCliRunGateway(config: CliEngineConfig): CliRunGateway = DesktopCliRunGateway(config)

private class DesktopCliRunGateway(
    private val config: CliEngineConfig,
) : CliRunGateway {
    private val backgroundContext = Dispatchers.IO
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val driver = buildDatabaseDriver(config.databaseDir)
    private val database = Database(driver)
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val fileSystem = FileSystem.SYSTEM

    // Mutated by run() before each run so the shared engine/submit lambdas observe the active flags.
    @Volatile
    private var currentOptions = CliRunOptions()

    private val networkTypeFinder = DesktopNetworkTypeFinder()

    private val resultRepository = ResultRepository(database, backgroundContext)
    private val measurementRepository = MeasurementRepository(database, json, backgroundContext)
    private val urlRepository = UrlRepository(database, backgroundContext)
    private val networkRepository = NetworkRepository(database, backgroundContext)
    private val testDescriptorRepository = TestDescriptorRepository(database, json, backgroundContext)
    private val preferenceRepository = PreferenceRepository(
        PreferenceDataStoreFactory.create(scope = scope) { File(config.preferencesFile()) },
    )

    private val platformInfo = PlatformInfo(
        buildName = config.softwareVersion,
        buildNumber = "0",
        platform = Platform.Desktop(config.osName),
        osVersion = config.osVersion,
        model = "cli",
        requestNotificationsPermission = false,
        sentryDsn = "",
    )

    private val coreConfig = CoreConfig(
        baseSoftwareName = config.baseSoftwareName,
        ooniApiBaseUrl = config.ooniApiBaseUrl,
        passportVersion = config.passportVersion,
        engineName = CLI_ENGINE_NAME,
    )

    private val getBatteryState: () -> BatteryState = { BatteryState.Charging }

    private val proxyOption: ProxyOption =
        config.proxy?.takeIf { it.isNotEmpty() }?.let(ProxyOption::Custom) ?: ProxyOption.None

    // CLI engine preferences: `--no-collector` disables uploads; proxy/geoip come from the config.
    private val getEnginePreferences: suspend () -> EnginePreferences = {
        EnginePreferences(
            enabledWebCategories = emptyList(),
            taskLogLevel = TaskLogLevel.Info,
            uploadResults = !currentOptions.noCollector,
            proxy = config.proxy,
            geoipDbPath = config.geoipDbPath,
            maxRuntime = null,
        )
    }

    private val engine = Engine(
        bridge = DesktopOonimkallBridge(),
        json = json,
        baseFilePath = config.baseFileDir,
        cacheDir = config.cacheDir,
        taskEventMapper = TaskEventMapper(networkTypeFinder, json),
        networkTypeFinder = networkTypeFinder,
        platformInfo = platformInfo,
        coreConfig = coreConfig,
        getEnginePreferences = getEnginePreferences,
        addRunCancelListener = { CancelListenerCallback {} },
        backgroundContext = backgroundContext,
    )

    private val passportBridge: PassportBridge = CliDesktopPassportBridge(userAgent = buildUserAgent())
    private val passportHttpClient = PassportHttpClient(
        passportGet = passportBridge,
        passportPost = passportBridge,
        passportAuthRegister = passportBridge,
        passportAuthSubmit = passportBridge,
        getProxyOption = { flowOf(proxyOption) },
        backgroundContext = backgroundContext,
        isOnline = { true },
    )

    // Only constructed if the with-credentials submit path is exercised; keeps native keychain
    // access out of construction and out of the `--no-creds` path.
    private val secureStorage by lazy {
        createDesktopSecureStorage(
            desktopOS = (platformInfo.platform as Platform.Desktop).os,
            appId = config.baseSoftwareName,
            baseSoftwareName = config.baseSoftwareName,
        )
    }

    private val buildCheckInRequest = BuildCheckInRequest(
        getEnginePreferences = getEnginePreferences,
        platformInfo = platformInfo,
        getBatteryState = getBatteryState,
        networkTypeFinder = networkTypeFinder,
        coreConfig = coreConfig,
    )
    private val checkIn = CheckIn(
        passportPost = passportHttpClient::post,
        buildCheckInRequest = buildCheckInRequest::invoke,
        json = json,
        setPreferenceByKey = preferenceRepository::setValueByKey,
        storeUrlsByUrl = urlRepository::createOrUpdateByUrl,
        ooniApiBaseUrl = config.ooniApiBaseUrl,
    )

    // Credential chain (with-credentials path). Reused, never duplicated.
    private val getManifest = GetManifest(getPreference = preferenceRepository::getValueByKey, json = json)
    private val getCredential = GetCredential(readSecureStorage = { key -> secureStorage.read(key) }, json = json)
    private val setCredential = SetCredential(writeSecureStorage = { key, value -> secureStorage.write(key, value) }, json = json)
    private val clearCredential = ClearCredential(deleteSecureStorage = { key -> secureStorage.delete(key) })
    private val registerUser = RegisterUser(
        userAuthRegister = passportHttpClient::userAuthRegister,
        setCredential = setCredential::invoke,
        backgroundContext = backgroundContext,
        json = json,
        ooniApiBaseUrl = config.ooniApiBaseUrl,
    )
    private val retrieveManifest = RetrieveManifest(
        passportGet = { url -> passportHttpClient.get(url) },
        setPreference = preferenceRepository::setValueByKey,
        json = json,
        backgroundContext = backgroundContext,
        ooniApiBaseUrl = config.ooniApiBaseUrl,
    )
    private val prepareAnonymousCredentials = PrepareAnonymousCredentials(
        getManifest = getManifest::invoke,
        retrieveManifest = retrieveManifest::invoke,
        getCredential = getCredential::invoke,
        registerUser = registerUser::invoke,
        backgroundContext = backgroundContext,
    )
    private val stampMeasurement = StampMeasurement(
        passportGetProbeId = passportBridge,
        getCredential = getCredential::invoke,
        json = json,
    )
    private val resolveSubmissionPolicy = ResolveSubmissionPolicy()
    private val submitMeasurementWithUser = SubmitMeasurementWithUser(
        getManifest = getManifest::invoke,
        getCredential = getCredential::invoke,
        setCredential = setCredential,
        stampMeasurement = stampMeasurement,
        resolveSubmissionPolicy = resolveSubmissionPolicy,
        userAuthSubmit = passportHttpClient::userAuthSubmit,
        json = json,
        ooniApiBaseUrl = config.ooniApiBaseUrl,
    )
    private val handleSubmitOutcome = HandleSubmitOutcome(
        retrieveManifest = { retrieveManifest() },
        clearCredential = clearCredential,
        signalUpdateRequired = {},
    )

    private val deleteFiles = DeleteFilesOkio(fileSystem, config.baseFileDir, backgroundContext)
    private val writeFile = WriteFileOkio(fileSystem, config.baseFileDir)

    private val submitMeasurement = SubmitMeasurement(
        // `--no-creds` routes submission through the anonymous engine-submit path: never call the
        // user-credential submit, never read/write secure-storage credentials.
        submitMeasurementWithUser = { data ->
            if (currentOptions.noCreds) Failure(null) else submitMeasurementWithUser(data)
        },
        engineSubmit = engine::submitMeasurement,
        readFile = ReadFileOkio(fileSystem, config.baseFileDir),
        deleteFiles = deleteFiles,
        updateMeasurement = measurementRepository::createOrUpdate,
        deleteMeasurementById = measurementRepository::deleteById,
        handleSubmitOutcome = handleSubmitOutcome::invoke,
        json = json,
    )

    private val getMeasurementsNotUploaded = GetMeasurementsNotUploaded(
        listMeasurementsNotUploaded = measurementRepository::listNotUploaded,
        getMeasurementById = measurementRepository::getById,
    )
    private val uploadMissingMeasurements = UploadMissingMeasurements(
        getMeasurementsNotUploaded = getMeasurementsNotUploaded::invoke,
        submitMeasurement = submitMeasurement::invoke,
    )

    private val runBackgroundStateManager = RunBackgroundStateManager()
    private val testProxy = TestProxy(
        getProxyOption = { flowOf(proxyOption) },
        passportGet = { url, proxy -> passportHttpClient.get(url, proxy) },
        backgroundContext = backgroundContext,
        ooniApiBaseUrl = config.ooniApiBaseUrl,
    )
    private val getFallbackUrls = GetFallbackUrls(
        getMeasurementsWithUrl = measurementRepository::listWithUrl,
        getBatteryState = getBatteryState,
    )
    private val finishInProgressData = FinishInProgressData(resultRepository::markAllAsDone)

    private val descriptorAssets: DescriptorAssetProvider = ClasspathDescriptorAssetProvider()
    private val descriptorDecoderJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val descriptorsMutex = Mutex()

    @Volatile
    private var cachedDescriptors: List<Descriptor>? = null

    private val getTestDescriptorsBySpec = GetTestDescriptorsBySpec(
        getTestDescriptors = { flow { emit(loadBootstrapDescriptors().map { it.toDescriptorItem() }) } },
    )

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

    private val runDescriptors = RunDescriptors(
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
        getEnginePreferences = getEnginePreferences,
        finishInProgressData = finishInProgressData::invoke,
        networkTypeFinder = networkTypeFinder::invoke,
        testProxy = testProxy::invoke,
    )

    // Rerun input restoration (result lookup) is wired by the CLI `run` command in T9; the
    // orchestrator already routes RunSpecification.Rerun through this hook.
    private val getRerunSpecification: suspend (RunSpecification.Rerun) -> RunSpecification.Full? = { null }

    private val orchestrator = CliRunOrchestrator(
        getPreferenceValueByKey = preferenceRepository::getValueByKey,
        prepareAnonymousCredentials = prepareAnonymousCredentials::invoke,
        uploadMissingMeasurements = uploadMissingMeasurements::invoke,
        runDescriptors = runDescriptors::invoke,
        getRerunSpecification = getRerunSpecification,
        setRunBackgroundState = runBackgroundStateManager::updateState,
        getRunBackgroundState = runBackgroundStateManager::observeState,
        addRunCancelListener = runBackgroundStateManager::addCancelListener,
    )

    override fun run(
        spec: RunSpecification,
        options: CliRunOptions,
    ): Flow<CliRunProgress> =
        flow {
            currentOptions = options
            // Gate the upload paths (RunBackgroundTask + RunNetTest read this preference).
            preferenceRepository.setValueByKey(SettingsKey.UPLOAD_RESULTS, !options.noCollector)
            // Persist the bootstrap descriptors so run result rows satisfy the Result -> TestDescriptor
            // foreign key (the CLI has no installed DB descriptors).
            testDescriptorRepository.createOrIgnore(loadBootstrapDescriptors())
            emitAll(orchestrator.run(spec, options))
        }

    override suspend fun descriptors(): List<Descriptor> = loadBootstrapDescriptors()

    // Route cancellation through the canonical path: RunBackgroundStateManager.cancel() fires the
    // registered run-cancel listeners (RunBackgroundTask/RunDescriptors), which move the state to
    // Stopping and cancel their jobs so run() completes gracefully with cleanup preserved.
    override fun cancel() {
        runBackgroundStateManager.cancel()
    }

    private suspend fun loadBootstrapDescriptors(): List<Descriptor> {
        cachedDescriptors?.let { return it }
        return descriptorsMutex.withLock {
            cachedDescriptors ?: BootstrapDescriptorDecoder(descriptorDecoderJson)
                .decode(descriptorAssets, DescriptorAssetSet.cliDefault)
                .also { cachedDescriptors = it }
        }
    }

    override fun close() {
        runCatching { driver.close() }
        scope.cancel()
    }

    private fun buildUserAgent(): String {
        val platformName = when ((platformInfo.platform as Platform.Desktop).os) {
            org.ooni.probe.shared.DesktopOS.Mac -> "macos"
            org.ooni.probe.shared.DesktopOS.Windows -> "windows"
            org.ooni.probe.shared.DesktopOS.Linux -> "linux"
            org.ooni.probe.shared.DesktopOS.Other -> "desktop"
        }
        return "ooni-passport-${config.passportVersion}; " +
            "${config.baseSoftwareName}-${config.softwareVersion}; " +
            platformName
    }
}

// The upload gateway's CliEngineConfig has no preferences path (it never touches DataStore); the run
// gateway derives one next to the database so onboarding/upload preferences persist per CLI home.
private fun CliEngineConfig.preferencesFile(): String = File(databaseDir, "probe.preferences_pb").path
