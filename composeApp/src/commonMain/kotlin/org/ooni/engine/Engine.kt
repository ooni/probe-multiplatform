package org.ooni.engine

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
) {
    fun startTask(taskSettings: TaskSettings): Flow<TaskEvent> =
        channelFlow {
            val finalSettings =
                taskSettings.copy(
                    stateDir = baseFilePath,
                    tunnelDir = baseFilePath,
                    tempDir = baseFilePath,
                    assetsDir = baseFilePath,
                )

            val response = httpDo(finalSettings)
            println(response)

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
            object : OonimkallBridge.SessionConfig {
                override val softwareName: String
                    get() = finalSettings.options.softwareName
                override val softwareVersion: String
                    get() = finalSettings.options.softwareVersion
                override val proxy: String?
                    get() = null
                override val probeServicesURL: String?
                    get() = "https://api.prod.ooni.io"
                override val assetsDir: String
                    get() = finalSettings.assetsDir.toString()
                override val stateDir: String
                    get() = finalSettings.stateDir.toString()
                override val tempDir: String
                    get() = finalSettings.tempDir.toString()
                override val tunnelDir: String
                    get() = finalSettings.tunnelDir.toString()
                override val logger: OonimkallBridge.Logger?
                    get() =
                        object : OonimkallBridge.Logger {
                            override fun debug(msg: String?) {
                                println("DEBUG: $msg")
                            }

                            override fun info(msg: String?) {
                                println("INFO: $msg")
                            }

                            override fun warn(msg: String?) {
                                println("WARN: $msg")
                            }
                        }
                override val verbose: Boolean
                    get() = true
            },
        )
    }

    suspend fun checkIn(finalSettings: TaskSettings): OonimkallBridge.CheckInResults? {
        return withContext(Dispatchers.IO) {
            return@withContext session(finalSettings).checkIn(
                object : OonimkallBridge.CheckInConfig {
                    override val charging: Boolean
                        get() = true
                    override val onWiFi: Boolean
                        get() = true
                    override val platform: String
                        get() = "android"
                    override val runType: String
                        get() = "autorun"
                    override val softwareName: String
                        get() = "ooniprobe-android-unattended"
                    override val softwareVersion: String
                        get() = "3.8.8"
                    override val webConnectivityCategories: List<String>
                        get() = listOf("NEWS")
                },
            )
        }
    }

    suspend fun httpDo(finalSettings: TaskSettings): String? {
        return withContext(Dispatchers.IO) {
            return@withContext session(finalSettings).httpDo(
                object : OonimkallBridge.HTTPRequest {
                    override val url: String
                        get() = "https://api.dev.ooni.io/api/v2/oonirun/links/10426"
                    override val method: String
                        get() = "GET"
                },
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
