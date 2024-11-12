package org.ooni.engine

import androidx.annotation.VisibleForTesting
import co.touchlab.kermit.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.take
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
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.shared.PlatformInfo
import org.ooni.probe.shared.value
import kotlin.coroutines.CoroutineContext

class Engine(
    @VisibleForTesting
    var bridge: OonimkallBridge,
    private val json: Json,
    private val baseFilePath: String,
    private val cacheDir: String,
    private val taskEventMapper: TaskEventMapper,
    private val networkTypeFinder: NetworkTypeFinder,
    private val isBatteryCharging: suspend () -> Boolean,
    private val platformInfo: PlatformInfo,
    private val getEnginePreferences: suspend () -> EnginePreferences,
    private val observeCancelTestRun: () -> Flow<Unit>,
    private val backgroundContext: CoroutineContext,
) {
    fun startTask(
        name: String,
        inputs: List<String>?,
        taskOrigin: TaskOrigin,
        descriptorId: InstalledTestDescriptorModel.Id?,
    ): Flow<TaskEvent> =
        channelFlow {
            val preferences = getEnginePreferences()
            val taskSettings = buildTaskSettings(name, inputs, taskOrigin, preferences, descriptorId)
            val settingsSerialized = json.encodeToString(taskSettings)

            var task: OonimkallBridge.Task? = null
            try {
                task = bridge.startTask(settingsSerialized)

                val cancelJob = async {
                    observeCancelTestRun()
                        .take(1)
                        .collect {
                            task.interrupt()
                        }
                }

                while (!task.isDone() && isActive) {
                    val eventJson = task.waitForNextEvent()
                    val taskEventResult = json.decodeFromString<TaskEventResult>(eventJson)
                    taskEventMapper(taskEventResult)?.let { send(it) }
                }

                if (cancelJob.isActive) {
                    cancelJob.cancel()
                }
            } catch (e: Exception) {
                Logger.d("Error while running task", e)
                throw MkException(e)
            } finally {
                if (task?.isDone() == false) {
                    task.interrupt()
                }
            }
        }.flowOn(backgroundContext)

    suspend fun submitMeasurements(
        measurement: String,
        taskOrigin: TaskOrigin = TaskOrigin.OoniRun,
    ): Result<SubmitMeasurementResults, MkException> =
        resultOf(backgroundContext) {
            val sessionConfig = buildSessionConfig(taskOrigin, getEnginePreferences())
            session(sessionConfig).submitMeasurement(measurement)
        }.mapError { MkException(it) }

    suspend fun checkIn(taskOrigin: TaskOrigin): Result<OonimkallBridge.CheckInResults, MkException> =
        resultOf(backgroundContext) {
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
        resultOf(backgroundContext) {
            session(buildSessionConfig(taskOrigin, getEnginePreferences()))
                .httpDo(
                    OonimkallBridge.HTTPRequest(
                        method = method,
                        url = url,
                    ),
                ).body
        }.mapError {
            MkException(it)
        }

    private fun session(sessionConfig: OonimkallBridge.SessionConfig): OonimkallBridge.Session = bridge.newSession(sessionConfig)

    private fun buildTaskSettings(
        name: String,
        inputs: List<String>?,
        taskOrigin: TaskOrigin,
        preferences: EnginePreferences,
        descriptorId: InstalledTestDescriptorModel.Id?,
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
            softwareVersion = platformInfo.buildName,
            maxRuntime = preferences.maxRuntime?.inWholeSeconds?.toInt() ?: -1,
        ),
        annotations = TaskSettings.Annotations(
            networkType = networkTypeFinder(),
            flavor = buildSoftwareName(taskOrigin),
            origin = taskOrigin,
            ooniRunLinkId = descriptorId?.value?.toString() ?: "",
        ),
        proxy = preferences.proxy,
    )

    private fun buildSessionConfig(
        taskOrigin: TaskOrigin,
        preferences: EnginePreferences,
    ) = OonimkallBridge.SessionConfig(
        softwareName = buildSoftwareName(taskOrigin),
        softwareVersion = platformInfo.buildName,
        proxy = preferences.proxy,
        probeServicesURL = OrganizationConfig.ooniApiBaseUrl,
        stateDir = "$baseFilePath/state",
        tunnelDir = "$baseFilePath/tunnel",
        tempDir = cacheDir,
        assetsDir = "$baseFilePath/assets",
        logger = oonimkallLogger,
        verbose = false,
    )

    private fun buildSoftwareName(taskOrigin: TaskOrigin) =
        OrganizationConfig.baseSoftwareName +
            "-" +
            platformInfo.platform.value +
            (if (taskOrigin == TaskOrigin.AutoRun) "-" + "unattended" else "")

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
