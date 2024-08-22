package org.ooni.engine

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.ooni.engine.OonimkallBridge.SubmitMeasurementResults
import org.ooni.engine.models.EnginePreferences
import org.ooni.engine.models.NetworkType
import org.ooni.engine.models.Result
import org.ooni.engine.models.TaskEvent
import org.ooni.engine.models.TaskEventResult
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TaskSettings
import org.ooni.engine.models.resultOf
import org.ooni.probe.config.Config
import org.ooni.probe.shared.Platform
import org.ooni.probe.shared.PlatformInfo

class Engine(
    private val bridge: OonimkallBridge,
    private val json: Json,
    private val baseFilePath: String,
    private val cacheDir: String,
    private val taskEventMapper: TaskEventMapper,
    private val networkTypeFinder: NetworkTypeFinder,
    private val isBatteryCharging: suspend () -> Boolean,
    private val platformInfo: PlatformInfo,
    private val getEnginePreferences: suspend () -> EnginePreferences,
    private val backgroundDispatcher: CoroutineDispatcher,
) {
    fun startTask(
        name: String,
        inputs: List<String>?,
        taskOrigin: TaskOrigin,
    ): Flow<TaskEvent> =
        channelFlow {
            val preferences = getEnginePreferences()
            val taskSettings = buildTaskSettings(name, inputs, taskOrigin, preferences)
            val settingsSerialized = json.encodeToString(taskSettings)

            var task: OonimkallBridge.Task? = null
            try {
                task = bridge.startTask(settingsSerialized)

                while (!task.isDone() && isActive) {
                    val eventJson = task.waitForNextEvent()
                    val taskEventResult = json.decodeFromString<TaskEventResult>(eventJson)
                    taskEventMapper(taskEventResult)?.let { send(it) }
                }
            } catch (e: CancellationException) {
                Logger.d("Test cancelled")
                throw e
            } catch (e: Exception) {
                Logger.d("Error while running task", e)
                throw MkException(e)
            } finally {
                task?.interrupt()
            }
        }.flowOn(backgroundDispatcher)

    suspend fun submitMeasurements(
        measurement: String,
        taskOrigin: TaskOrigin = TaskOrigin.OoniRun,
    ): Result<SubmitMeasurementResults, MkException> =
        resultOf(backgroundDispatcher) {
            val sessionConfig = buildSessionConfig(taskOrigin, getEnginePreferences())
            session(sessionConfig).submitMeasurement(measurement)
        }.mapError { MkException(it) }

    suspend fun checkIn(taskOrigin: TaskOrigin): Result<OonimkallBridge.CheckInResults, MkException> =
        resultOf(backgroundDispatcher) {
            val preferences = getEnginePreferences()
            val sessionConfig = buildSessionConfig(taskOrigin, preferences)
            session(sessionConfig).checkIn(
                OonimkallBridge.CheckInConfig(
                    charging = isBatteryCharging(),
                    onWiFi = networkTypeFinder() == NetworkType.Wifi,
                    platform = platformInfo.platform.value,
                    runType = taskOrigin.value,
                    softwareName = sessionConfig.softwareName,
                    softwareVersion = sessionConfig.softwareVersion,
                    webConnectivityCategories = preferences.enabledWebCategories,
                ),
            )
        }.mapError { MkException(it) }

    suspend fun httpDo(
        method: String,
        url: String,
        taskOrigin: TaskOrigin = TaskOrigin.OoniRun,
    ): Result<String?, MkException> =
        resultOf(backgroundDispatcher) {
            session(buildSessionConfig(taskOrigin, getEnginePreferences()))
                .httpDo(
                    OonimkallBridge.HTTPRequest(
                        method = method,
                        url = url,
                    ),
                ).body
        }.mapError { MkException(it) }

    private fun session(sessionConfig: OonimkallBridge.SessionConfig): OonimkallBridge.Session = bridge.newSession(sessionConfig)

    private fun buildTaskSettings(
        name: String,
        inputs: List<String>?,
        taskOrigin: TaskOrigin,
        preferences: EnginePreferences,
    ) = TaskSettings(
        name = name,
        inputs = inputs.orEmpty(),
        disabledEvents = listOf(
            "status.queued",
            "status.update.websites",
            "failure.report_close",
        ),
        logLevel = preferences.taskLogLevel,
        stateDir = "$baseFilePath/state",
        tunnelDir = "$baseFilePath/tunnel",
        tempDir = cacheDir,
        assetsDir = "$baseFilePath/assets",
        options = TaskSettings.Options(
            noCollector = !preferences.uploadResults,
            softwareName = buildSoftwareName(taskOrigin),
            softwareVersion = platformInfo.version,
            maxRuntime = preferences.maxRuntime?.inWholeSeconds?.toInt() ?: -1,
        ),
        annotations = TaskSettings.Annotations(
            networkType = networkTypeFinder(),
            flavor = buildSoftwareName(taskOrigin),
            origin = taskOrigin,
        ),
        proxy = preferences.proxy,
    )

    private fun buildSessionConfig(
        taskOrigin: TaskOrigin,
        preferences: EnginePreferences,
    ) = OonimkallBridge.SessionConfig(
        softwareName = buildSoftwareName(taskOrigin),
        softwareVersion = platformInfo.version,
        proxy = preferences.proxy,
        probeServicesURL = Config.OONI_API_BASE_URL,
        stateDir = "$baseFilePath/state",
        tunnelDir = "$baseFilePath/tunnel",
        tempDir = cacheDir,
        assetsDir = "$baseFilePath/assets",
        logger = oonimkallLogger,
        verbose = false,
    )

    private fun buildSoftwareName(taskOrigin: TaskOrigin) =
        Config.BASE_SOFTWARE_NAME +
            "-" +
            platformInfo.platform.value +
            "-" +
            (if (taskOrigin == TaskOrigin.AutoRun) "-" + "unattended" else "")

    private val Platform.value
        get() =
            when (this) {
                Platform.Android -> "android"
                Platform.Ios -> "ios"
            }

    private val oonimkallLogger by lazy {
        object : OonimkallBridge.Logger {
            override fun debug(msg: String?) {
                msg?.let { Logger.d(it) }
            }

            override fun info(msg: String?) {
                msg?.let { Logger.d(it) }
            }

            override fun warn(msg: String?) {
                msg?.let { Logger.d(it) }
            }
        }
    }

    class MkException(t: Throwable) : Exception(t)
}
