package org.ooni.engine

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

            val task = bridge.startTask(json.encodeToString(finalSettings))

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
