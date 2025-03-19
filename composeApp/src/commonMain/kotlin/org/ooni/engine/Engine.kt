package org.ooni.engine

import androidx.annotation.VisibleForTesting
import co.touchlab.kermit.Logger
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
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.shared.PlatformInfo
import org.ooni.probe.shared.value
import kotlin.coroutines.CoroutineContext

const val MAX_RUNTIME_DISABLED = -1

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
    private val addRunCancelListener: (() -> Unit) -> Unit,
    private val backgroundContext: CoroutineContext,
) {
    fun startTask(
        netTest: NetTest,
        taskOrigin: TaskOrigin,
        descriptorId: InstalledTestDescriptorModel.Id?,
    ): Flow<TaskEvent> =
        channelFlow {
            val preferences = getEnginePreferences()
            val taskSettings =
                buildTaskSettings(netTest, taskOrigin, preferences, descriptorId)
            val settingsSerialized = json.encodeToString(taskSettings)

            var task: OonimkallBridge.Task? = null
            var isCancelled = false
            try {
                task = bridge.startTask(settingsSerialized)

                addRunCancelListener {
                    isCancelled = true
                    task.interrupt()
                }

                while (!task.isDone() && isActive) {
                    val eventJson = task.waitForNextEvent()
                    val taskEventResult = json.decodeFromString<TaskEventResult>(eventJson)
                    taskEventMapper(taskEventResult, isCancelled)?.let { send(it) }
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

    suspend fun checkIn(taskOrigin: TaskOrigin): Result<OonimkallBridge.CheckInResults, MkException> {
        return resultOf(backgroundContext) {
            val preferences = getEnginePreferences()
            val sessionConfig = buildSessionConfig(taskOrigin, preferences)
            val session = session(sessionConfig)
            try {
                session.checkIn(
                    OonimkallBridge.CheckInConfig(
                        charging = isBatteryCharging(),
                        onWiFi = networkTypeFinder() == NetworkType.Wifi,
                        platform = platformInfo.platform.value,
                        runType = when (taskOrigin) {
                            TaskOrigin.AutoRun -> "timed"
                            TaskOrigin.OoniRun -> "manual"
                        },
                        softwareName = sessionConfig.softwareName,
                        softwareVersion = sessionConfig.softwareVersion,
                        webConnectivityCategories = preferences.enabledWebCategories,
                    ),
                )
            } finally {
                try {
                    session.close()
                } catch (e: Exception) {
                    Logger.w("Error closing session", e)
                }
            }
        }.mapError { MkException(it) }
    }

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
        netTest: NetTest,
        taskOrigin: TaskOrigin,
        preferences: EnginePreferences,
        descriptorId: InstalledTestDescriptorModel.Id?,
    ) = TaskSettings(
        name = netTest.test.name,
        inputs = netTest.inputs.orEmpty(),
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
            maxRuntime = maxRuntime(taskOrigin, netTest, preferences),
        ),
        annotations = TaskSettings.Annotations(
            networkType = networkTypeFinder(),
            flavor = buildSoftwareName(taskOrigin),
            origin = taskOrigin,
            osVersion = platformInfo.osVersion,
            ooniRunLinkId = descriptorId?.value ?: "",
        ),
        proxy = preferences.proxy,
    )

    private fun maxRuntime(
        taskOrigin: TaskOrigin,
        netTest: NetTest,
        preferences: EnginePreferences,
    ) = if (taskOrigin == TaskOrigin.AutoRun) {
        MAX_RUNTIME_DISABLED
    } else if (netTest.callCheckIn) {
        preferences.maxRuntime?.inWholeSeconds?.toInt()?.let { maxRuntimePreference ->
            if (maxRuntimePreference > 0) 30 + maxRuntimePreference else MAX_RUNTIME_DISABLED
        } ?: MAX_RUNTIME_DISABLED
    } else {
        MAX_RUNTIME_DISABLED
    }

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
