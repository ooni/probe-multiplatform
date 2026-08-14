package org.ooni.probe.core

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okio.FileSystem
import org.ooni.engine.Engine
import org.ooni.engine.OonimkallBridge.SubmitMeasurementResults
import org.ooni.engine.models.EnginePreferences
import org.ooni.engine.models.Failure
import org.ooni.engine.models.NetworkType
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.engine.models.TaskEvent
import org.ooni.engine.models.TaskLogLevel
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TestType
import org.ooni.probe.Database
import org.ooni.probe.data.buildDatabaseDriver
import org.ooni.probe.data.disk.DeleteFiles
import org.ooni.probe.data.disk.DeleteFilesOkio
import org.ooni.probe.data.disk.ReadFile
import org.ooni.probe.data.disk.WriteFileOkio
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.models.UpdateStatus
import org.ooni.probe.data.models.toDescriptorItem
import org.ooni.probe.data.repositories.MeasurementRepository
import org.ooni.probe.data.repositories.NetworkRepository
import org.ooni.probe.data.repositories.PreferenceRepository
import org.ooni.probe.data.repositories.ResultRepository
import org.ooni.probe.data.repositories.TestDescriptorRepository
import org.ooni.probe.data.repositories.UrlRepository
import org.ooni.probe.domain.CheckIn
import org.ooni.probe.domain.RunBackgroundStateManager
import org.ooni.probe.domain.RunDescriptors
import org.ooni.probe.domain.RunNetTest
import org.ooni.probe.domain.SubmitMeasurement
import org.ooni.probe.domain.UploadMissingMeasurements
import org.ooni.probe.domain.descriptors.GetTestDescriptorsBySpec
import org.ooni.probe.domain.proxy.TestProxy
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Exercises the CLI run orchestration through fakes (no engine or passport native library).
 *
 * The real [buildDesktopCliRunGateway] is NOT run end-to-end here: the passport native lib is absent
 * under `desktopTest`, so orchestration is validated via [CliRunOrchestrator] with fake lambdas, and
 * only a native-free construction/descriptor smoke test touches the real gateway.
 */
class DesktopCliRunGatewayTest {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Uses runBlocking (not runTest): the canonical chain runs the real repositories/flows on
    // Dispatchers.IO, which the virtual-time StandardTestDispatcher would not deterministically await.
    @Test
    fun fullSpecDrivesCanonicalRunChainAndPersistsResultAndMeasurement() =
        runBlocking {
            val dataDir = Files.createTempDirectory("cli-run-full")
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val driver = buildDatabaseDriver(dataDir.toString())
            val database = Database(driver)
            val backgroundContext = Dispatchers.IO

            val resultRepository = ResultRepository(database, backgroundContext)
            val measurementRepository = MeasurementRepository(database, json, backgroundContext)
            val urlRepository = UrlRepository(database, backgroundContext)
            val networkRepository = NetworkRepository(database, backgroundContext)
            val testDescriptorRepository = TestDescriptorRepository(database, json, backgroundContext)
            val preferenceRepository = PreferenceRepository(
                PreferenceDataStoreFactory.create(scope = scope) { dataDir.resolve("probe.preferences_pb").toFile() },
            )

            try {
                val netTest = NetTest(test = TestType.FacebookMessenger, inputs = emptyList())
                val descriptor = descriptor(netTest)
                val descriptorItem = descriptor.toDescriptorItem(UpdateStatus.Unknown)
                // Bootstrap descriptors must exist in the DB so run result rows satisfy the
                // Result -> TestDescriptor foreign key (same install the gateway performs).
                testDescriptorRepository.createOrIgnore(listOf(descriptor))

                val spec = RunSpecification.Full(
                    tests = listOf(
                        RunSpecification.Test(
                            descriptorId = descriptorItem.descriptor.id,
                            netTests = listOf(netTest),
                        ),
                    ),
                    taskOrigin = TaskOrigin.OoniRun,
                )

                // Fake engine: scripts the TaskEvents a real net test emits, so RunNetTest persists a
                // measurement row without any native call.
                val startTest: (NetTest, TaskOrigin, Descriptor.Id) -> Flow<TaskEvent> = { _, _, _ ->
                    listOf(
                        TaskEvent.Started,
                        TaskEvent.GeoIpLookup(
                            networkName = "Example Net",
                            ip = "1.2.3.4",
                            asn = "AS123",
                            countryCode = "US",
                            geoIpdb = null,
                            networkType = NetworkType.Wifi,
                        ),
                        TaskEvent.ReportCreate(reportId = "report-1"),
                        TaskEvent.MeasurementStart(index = 0, url = null),
                        TaskEvent.Measurement(index = 0, json = """{"probe":"cli"}""", result = null),
                        TaskEvent.MeasurementDone(index = 0),
                        TaskEvent.End(downloadedKb = 1, uploadedKb = 1),
                    ).asFlow()
                }

                val stateManager = RunBackgroundStateManager()
                val enginePreferences: suspend () -> EnginePreferences = {
                    EnginePreferences(
                        enabledWebCategories = emptyList(),
                        taskLogLevel = TaskLogLevel.Info,
                        uploadResults = false,
                        proxy = null,
                        geoipDbPath = null,
                        maxRuntime = null,
                    )
                }

                fun buildRunNetTest(runSpec: RunNetTest.Specification) =
                    RunNetTest(
                        startTest = startTest,
                        getResultByIdAndUpdate = resultRepository::getByIdAndUpdate,
                        setCurrentTestState = stateManager::updateState,
                        getOrCreateUrl = urlRepository::getOrCreateByUrl,
                        storeMeasurement = measurementRepository::createOrUpdate,
                        storeNetwork = networkRepository::createIfNew,
                        writeFile = WriteFileOkio(FileSystem.SYSTEM, dataDir.toString()),
                        deleteFiles = DeleteFilesOkio(FileSystem.SYSTEM, dataDir.toString(), backgroundContext),
                        json = json,
                        getPreferenceValueByKey = preferenceRepository::getValueByKey,
                        submitMeasurement = { null },
                        spec = runSpec,
                    )

                val runDescriptors = RunDescriptors(
                    getTestDescriptorsBySpec = GetTestDescriptorsBySpec { flowOf(listOf(descriptorItem)) }::invoke,
                    checkIn = { Failure(CheckIn.Unsuccessful(null)) },
                    getFallbackUrls = { emptyList() },
                    storeResult = resultRepository::createOrUpdate,
                    markResultAsDone = resultRepository::markAsDone,
                    getRunBackgroundState = stateManager.observeState(),
                    setRunBackgroundState = stateManager::updateState,
                    runNetTest = { buildRunNetTest(it)() },
                    cancelRun = stateManager::cancel,
                    addRunCancelListener = stateManager::addCancelListener,
                    reportTestRunError = stateManager::reportError,
                    getEnginePreferences = enginePreferences,
                    finishInProgressData = { resultRepository.markAllAsDone() },
                    networkTypeFinder = { NetworkType.Wifi },
                    testProxy = { flowOf(TestProxy.State.Available) },
                )

                var prepareCredentialsCalls = 0
                val orchestrator = CliRunOrchestrator(
                    getPreferenceValueByKey = preferenceRepository::getValueByKey,
                    prepareAnonymousCredentials = { prepareCredentialsCalls++ },
                    uploadMissingMeasurements = { emptyFlow() },
                    runDescriptors = runDescriptors::invoke,
                    getRerunSpecification = { null },
                    setRunBackgroundState = stateManager::updateState,
                    getRunBackgroundState = stateManager::observeState,
                    addRunCancelListener = stateManager::addCancelListener,
                )

                val progress = orchestrator.run(spec, CliRunOptions()).toList()

                // Progress states: the run entered RunningTests and finished. (Transient per-test states
                // are conflated by the underlying StateFlow, so the deterministic proof that the canonical
                // chain reached RunNetTest is the persisted measurement row asserted below.)
                assertTrue(progress.any { it.phase == CliRunProgress.Phase.RunningTests }, "expected RunningTests progress")
                assertTrue(progress.last().finished, "expected a terminal finished progress")
                assertEquals(1, prepareCredentialsCalls, "Full run prepares credentials once")

                // Result + measurement rows persisted by the RunDescriptors -> RunNetTest chain.
                val results = resultRepository.list().first()
                assertEquals(1, results.size)
                val resultId = results.first().result.id!!
                val measurements = measurementRepository.listByResultId(resultId).first()
                assertEquals(1, measurements.size, "RunNetTest must persist one measurement row")
                assertEquals(TestType.FacebookMessenger, measurements.first().measurement.test)
            } finally {
                driver.close()
                scope.cancel()
                dataDir.toFile().deleteRecursively()
            }
        }

    @Test
    fun onlyUploadMissingResultsShortCircuitsWithoutRunningTests() =
        runTest {
            val stateManager = RunBackgroundStateManager()
            var runDescriptorsCalls = 0
            var uploadCalls = 0

            val orchestrator = CliRunOrchestrator(
                // Upload gate: RunBackgroundTask only uploads when UPLOAD_RESULTS is true.
                getPreferenceValueByKey = { key -> flowOf(if (key == SettingsKey.UPLOAD_RESULTS) true else null) },
                prepareAnonymousCredentials = {},
                uploadMissingMeasurements = {
                    uploadCalls++
                    flowOf(
                        UploadMissingMeasurements.State.Starting,
                        UploadMissingMeasurements.State.Uploading(0, 0, 2),
                        UploadMissingMeasurements.State.Finished(2, 0, 2),
                    )
                },
                runDescriptors = { runDescriptorsCalls++ },
                getRerunSpecification = { null },
                setRunBackgroundState = stateManager::updateState,
                getRunBackgroundState = stateManager::observeState,
                addRunCancelListener = stateManager::addCancelListener,
            )

            val progress = orchestrator.run(RunSpecification.OnlyUploadMissingResults, CliRunOptions()).toList()

            assertEquals(0, runDescriptorsCalls, "upload-only must not start a fresh measurement")
            assertEquals(1, uploadCalls, "upload-only must upload missing measurements")
            val uploadProgress = progress.filter { it.phase == CliRunProgress.Phase.UploadingMissingResults }
            assertTrue(uploadProgress.isNotEmpty(), "expected upload progress states")
            assertEquals(2, uploadProgress.last().total)
            assertTrue(progress.last().finished, "expected a terminal finished progress")
        }

    @Test
    fun noCredsDoesNotPrepareAnonymousCredentials() =
        runTest {
            suspend fun prepareCallsFor(noCreds: Boolean): Int {
                val stateManager = RunBackgroundStateManager()
                var prepareCalls = 0
                val orchestrator = CliRunOrchestrator(
                    getPreferenceValueByKey = { key -> flowOf(if (key == SettingsKey.UPLOAD_RESULTS) true else null) },
                    prepareAnonymousCredentials = { prepareCalls++ },
                    uploadMissingMeasurements = { flowOf(UploadMissingMeasurements.State.Finished(0, 0, 0)) },
                    runDescriptors = {},
                    getRerunSpecification = { null },
                    setRunBackgroundState = stateManager::updateState,
                    getRunBackgroundState = stateManager::observeState,
                    addRunCancelListener = stateManager::addCancelListener,
                )
                orchestrator.run(RunSpecification.OnlyUploadMissingResults, CliRunOptions(noCreds = noCreds)).toList()
                return prepareCalls
            }

            assertEquals(0, prepareCallsFor(noCreds = true), "--no-creds must never prepare anonymous credentials")
            assertEquals(1, prepareCallsFor(noCreds = false), "the default path prepares anonymous credentials")
        }

    @Test
    fun noCredsRoutesSubmissionThroughAnonymousEngineWithoutUserSubmitOrSecureStorage() =
        runTest {
            // Mirrors DesktopCliRunGateway's submit routing: `--no-creds` short-circuits the
            // user-credential submit (and its secure-storage credential read) so SubmitMeasurement falls
            // back to the anonymous engine-submit path.
            var userSubmitCalls = 0
            var secureStorageReads = 0
            var engineSubmitCalls = 0

            val recordingSubmitWithUser: suspend (String) -> Result<SubmitMeasurement.ResponseData, Throwable?> = { _ ->
                userSubmitCalls++
                // The real SubmitMeasurementWithUser reads the stored credential; record that read here.
                secureStorageReads++
                Success(SubmitMeasurement.ResponseData(uid = MeasurementModel.Uid("user-uid")))
            }
            val engineSubmit: suspend (String) -> Result<SubmitMeasurementResults, Engine.MkException> = { _ ->
                engineSubmitCalls++
                Success(SubmitMeasurementResults(updatedMeasurement = null, updatedReportId = "r", measurementUid = "anon-uid"))
            }

            fun buildSubmit(noCreds: Boolean) =
                SubmitMeasurement(
                    submitMeasurementWithUser = { data -> if (noCreds) Failure(null) else recordingSubmitWithUser(data) },
                    engineSubmit = engineSubmit,
                    readFile = ReadFile { "{}" },
                    deleteFiles = DeleteFiles { },
                    updateMeasurement = { },
                    deleteMeasurementById = { },
                    handleSubmitOutcome = { _, _ -> },
                    json = json,
                )

            val measurement = MeasurementModel(
                id = MeasurementModel.Id(1),
                test = TestType.FacebookMessenger,
                reportId = MeasurementModel.ReportId("report-1"),
                urlId = null,
                resultId = ResultModel.Id(1),
            )

            val noCredsResult = buildSubmit(noCreds = true).invokeInstrumented(measurement)
            assertEquals(0, userSubmitCalls, "--no-creds must not call the user-credential submit")
            assertEquals(0, secureStorageReads, "--no-creds must not read secure-storage credentials")
            assertEquals(1, engineSubmitCalls, "--no-creds routes through the anonymous engine submit")
            assertTrue(noCredsResult?.isUploaded == true)

            userSubmitCalls = 0
            secureStorageReads = 0
            engineSubmitCalls = 0

            buildSubmit(noCreds = false).invokeInstrumented(measurement)
            assertEquals(1, userSubmitCalls, "the default path uses the user-credential submit")
            assertEquals(0, engineSubmitCalls, "the user-credential submit succeeded, so no engine fallback")
        }

    @Test
    fun buildDesktopCliRunGatewayExposesRunSurfaceAndDescriptorsWithoutNative() =
        runTest {
            val dataDir = Files.createTempDirectory("cli-run-smoke")
            try {
                val gateway = buildDesktopCliRunGateway(
                    CliEngineConfig(
                        databaseDir = dataDir.toString(),
                        baseFileDir = dataDir.toString(),
                        cacheDir = dataDir.toString(),
                        ooniApiBaseUrl = "https://api.ooni.io",
                        baseSoftwareName = "ooniprobe",
                        softwareVersion = "test",
                        passportVersion = "test",
                        proxy = null,
                        osName = System.getProperty("os.name") ?: "unknown",
                        osVersion = System.getProperty("os.version") ?: "unknown",
                    ),
                )
                try {
                    // Construction + descriptor loading is native-free (no engine/passport call).
                    val descriptors = gateway.descriptors()
                    assertTrue(descriptors.isNotEmpty(), "CLI exposes the bundled OONI bootstrap descriptors")

                    // The gateway is the only exposed run surface: run() returns a cold projection flow.
                    val runFlow: Flow<CliRunProgress> = gateway.run(RunSpecification.OnlyUploadMissingResults)
                    assertFalse(runFlow == emptyFlow<CliRunProgress>())
                } finally {
                    gateway.close()
                }
            } finally {
                File(dataDir.toString()).deleteRecursively()
            }
        }

    private fun descriptor(vararg netTests: NetTest) =
        Descriptor(
            id = Descriptor.Id("cli-run-test"),
            revision = 1,
            name = "CLI Run Test",
            shortDescription = null,
            description = null,
            author = null,
            netTests = netTests.toList(),
            nameIntl = null,
            shortDescriptionIntl = null,
            descriptionIntl = null,
            icon = null,
            color = null,
            animation = null,
            expirationDate = null,
            dateCreated = null,
            dateUpdated = null,
            dateInstalled = null,
            autoUpdate = false,
        )
}
