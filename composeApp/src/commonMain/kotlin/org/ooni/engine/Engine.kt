package org.ooni.engine

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.ooni.engine.models.EventResult
import org.ooni.engine.models.TaskEvent
import org.ooni.engine.models.TaskSettings
import kotlin.math.roundToInt

class Engine(
    private val bridge: OonimkallBridge,
    private val json: Json,
    private val baseFilePath: String,
    private val cacheDir: String,
) {
    fun startTask(taskSettings: TaskSettings): Flow<TaskEvent> =
        channelFlow {
            val finalSettings =
                taskSettings.copy(
                    stateDir = "$baseFilePath/state",
                    tunnelDir = "$baseFilePath/tunnel",
                    tempDir = cacheDir,
                    assetsDir = "$baseFilePath/assets",
                )

            val response = httpDo(finalSettings)
            response?.let {
                Logger.d(it)
            }

            val checkinResults = checkIn(finalSettings)

            val task =
                bridge.startTask(
                    json.encodeToString(
                        checkinResults?.urls?.map { it.url }?.let {
                            finalSettings.copy(
                                inputs = it,
                                options =
                                    finalSettings.options.copy(
                                        maxRuntime = 90,
                                    ),
                            )
                        } ?: finalSettings,
                    ),
                )

            while (!task.isDone()) {
                val eventJson = task.waitForNextEvent()
                val eventResult = json.decodeFromString<EventResult>(eventJson)
                eventResult.toTaskEvent()?.let { send(it) }
            }

            invokeOnClose {
                if (it is CancellationException) {
                    task.interrupt()
                }
            }
        }

    fun session(finalSettings: TaskSettings): OonimkallBridge.Session {
        return bridge.newSession(
            OonimkallBridge.SessionConfig(
                softwareName = finalSettings.options.softwareName,
                softwareVersion = finalSettings.options.softwareVersion,
                proxy = null,
                probeServicesURL = "https://api.prod.ooni.io",
                assetsDir = finalSettings.assetsDir.toString(),
                stateDir = finalSettings.stateDir.toString(),
                tempDir = finalSettings.tempDir.toString(),
                tunnelDir = finalSettings.tunnelDir.toString(),
                logger =
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
                    },
                verbose = true,
            ),
        )
    }

    suspend fun checkIn(finalSettings: TaskSettings): OonimkallBridge.CheckInResults? {
        return withContext(Dispatchers.IO) {
            return@withContext session(finalSettings).checkIn(
                OonimkallBridge.CheckInConfig(
                    charging = true,
                    onWiFi = true,
                    platform = "android",
                    runType = "autorun",
                    softwareName = "ooniprobe-android-unattended",
                    softwareVersion = "3.8.8",
                    webConnectivityCategories = listOf("NEWS"),
                ),
            )
        }
    }

    suspend fun httpDo(finalSettings: TaskSettings): String? {
        return withContext(Dispatchers.IO) {
            return@withContext session(finalSettings).httpDo(
                OonimkallBridge.HTTPRequest(
                    url = "https://api.dev.ooni.io/api/v2/oonirun/links/10426",
                    method = "GET",
                ),
            ).body
        }
    }

    private fun EventResult.toTaskEvent(): TaskEvent? =
        when (key) {
            "status.started" -> TaskEvent.Started

            "status.end" -> TaskEvent.StatusEnd

            "status.progress" ->
                value?.percentage?.let { percentageValue ->
                    TaskEvent.Progress(
                        percentage = (percentageValue * 100.0).roundToInt(),
                        message = value?.message,
                    )
                }

            "log" ->
                value?.message?.let { message ->
                    TaskEvent.Log(
                        level = value?.logLevel,
                        message = message,
                    )
                }

            "status.report_create" ->
                value?.reportId?.let {
                    TaskEvent.ReportCreate(reportId = it)
                }

            "task_terminated" -> TaskEvent.TaskTerminated

            "failure.startup" -> TaskEvent.FailureStartup(message = value?.failure)

            else -> null
        }
}
