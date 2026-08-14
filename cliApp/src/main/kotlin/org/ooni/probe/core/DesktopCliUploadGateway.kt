package org.ooni.probe.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okio.FileSystem
import org.ooni.engine.DesktopNetworkTypeFinder
import org.ooni.engine.DesktopOonimkallBridge
import org.ooni.engine.Engine
import org.ooni.engine.TaskEventMapper
import org.ooni.engine.models.EnginePreferences
import org.ooni.engine.models.Failure
import org.ooni.engine.models.TaskLogLevel
import org.ooni.probe.Database
import org.ooni.probe.config.CoreConfig
import org.ooni.probe.data.buildDatabaseDriver
import org.ooni.probe.data.disk.DeleteFilesOkio
import org.ooni.probe.data.disk.ReadFileOkio
import org.ooni.probe.data.models.MeasurementsFilter
import org.ooni.probe.data.repositories.MeasurementRepository
import org.ooni.probe.domain.CancelListenerCallback
import org.ooni.probe.domain.GetMeasurementsNotUploaded
import org.ooni.probe.domain.SubmitMeasurement
import org.ooni.probe.domain.UploadMissingMeasurements
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo

/** Builds the desktop/JVM CLI upload gateway (anonymous engine-submit path). */
fun buildDesktopCliUploadGateway(config: CliEngineConfig): CliUploadGateway = DesktopCliUploadGateway(config)

private class DesktopCliUploadGateway(
    config: CliEngineConfig,
) : CliUploadGateway {
    private val backgroundContext = Dispatchers.IO

    // Owns the in-flight upload collection so cancel() can interrupt it (including mid-submit)
    // without bypassing cleanup; close() cancels the scope and closes the driver.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val driver = buildDatabaseDriver(config.databaseDir)
    private val database = Database(driver)
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val fileSystem = FileSystem.SYSTEM
    private val networkTypeFinder = DesktopNetworkTypeFinder()

    private val measurementRepository = MeasurementRepository(database, json, backgroundContext)

    private val platformInfo = PlatformInfo(
        buildName = config.softwareVersion,
        buildNumber = "0",
        platform = Platform.Desktop(config.osName),
        osVersion = config.osVersion,
        model = "cli",
        requestNotificationsPermission = false,
        sentryDsn = "",
    )

    private val engine = Engine(
        bridge = DesktopOonimkallBridge(),
        json = json,
        baseFilePath = config.baseFileDir,
        cacheDir = config.cacheDir,
        taskEventMapper = TaskEventMapper(networkTypeFinder, json),
        networkTypeFinder = networkTypeFinder,
        platformInfo = platformInfo,
        coreConfig = CoreConfig(
            baseSoftwareName = config.baseSoftwareName,
            ooniApiBaseUrl = config.ooniApiBaseUrl,
            passportVersion = config.passportVersion,
            engineName = CLI_ENGINE_NAME,
        ),
        getEnginePreferences = {
            EnginePreferences(
                enabledWebCategories = emptyList(),
                taskLogLevel = TaskLogLevel.Info,
                uploadResults = true,
                proxy = config.proxy,
                geoipDbPath = null,
                maxRuntime = null,
            )
        },
        addRunCancelListener = { CancelListenerCallback {} },
        backgroundContext = backgroundContext,
    )

    private val submitMeasurement = SubmitMeasurement(
        // Anonymous upload: skip user-credential submit and fall back to the engine submit path.
        submitMeasurementWithUser = { Failure(null) },
        engineSubmit = engine::submitMeasurement,
        readFile = ReadFileOkio(fileSystem, config.baseFileDir),
        deleteFiles = DeleteFilesOkio(fileSystem, config.baseFileDir, backgroundContext),
        updateMeasurement = measurementRepository::createOrUpdate,
        deleteMeasurementById = measurementRepository::deleteById,
        handleSubmitOutcome = { _, _ -> },
        json = json,
    )

    private val uploadMissingMeasurements = UploadMissingMeasurements(
        getMeasurementsNotUploaded = GetMeasurementsNotUploaded(
            listMeasurementsNotUploaded = measurementRepository::listNotUploaded,
            getMeasurementById = measurementRepository::getById,
        )::invoke,
        submitMeasurement = submitMeasurement::invoke,
    )

    // Runs the upload collection in the gateway-owned [scope] and bridges progress to the caller.
    // cancel() cancels the scope's child, ending the job and completing the flow gracefully.
    override fun uploadMissing(filter: MeasurementsFilter): Flow<CliUploadProgress> =
        channelFlow {
            val job = scope.launch {
                uploadMissingMeasurements(filter).collect { state ->
                    send(
                        when (state) {
                            UploadMissingMeasurements.State.Starting -> CliUploadProgress(0, 0, 0, finished = false)
                            is UploadMissingMeasurements.State.Uploading ->
                                CliUploadProgress(state.uploaded, state.failedToUpload, state.total, finished = false)
                            is UploadMissingMeasurements.State.Finished ->
                                CliUploadProgress(state.uploaded, state.failedToUpload, state.total, finished = true)
                        },
                    )
                }
            }
            job.join()
        }

    override fun cancel() {
        scope.coroutineContext.cancelChildren()
    }

    override fun close() {
        scope.cancel()
        runCatching { driver.close() }
    }
}
