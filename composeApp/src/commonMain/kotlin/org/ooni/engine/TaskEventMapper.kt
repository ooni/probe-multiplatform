package org.ooni.engine

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import org.ooni.engine.models.TaskEvent
import org.ooni.engine.models.TaskEventResult
import kotlin.math.roundToInt

class TaskEventMapper(
    private val networkTypeFinder: NetworkTypeFinder,
    private val json: Json,
) {
    operator fun invoke(result: TaskEventResult): TaskEvent? {
        val key = result.key
        val value = result.value

        return when (key) {
            "bug.json_dump" ->
                value?.let {
                    TaskEvent.BugJsonDump(value = value)
                } ?: run {
                    Logger.d("Task Event $key missing 'value'")
                    null
                }

            "failure.measurement_submission" ->
                TaskEvent.MeasurementSubmissionFailure(
                    index = value?.idx ?: 0,
                    message = value?.failure,
                )

            "failure.resolver_lookup" -> TaskEvent.ResolverLookupFailure(message = value?.failure)

            "failure.startup" -> TaskEvent.StartupFailure(message = value?.failure)

            "log" ->
                value?.message?.let { message ->
                    TaskEvent.Log(
                        level = value.logLevel,
                        message = message,
                    )
                } ?: run {
                    Logger.d("Task Event $key missing 'message'")
                    null
                }

            "measurement" ->
                value?.jsonStr?.let { jsonString ->
                    TaskEvent.Measurement(
                        index = value.idx,
                        json = jsonString,
                        result =
                            try {
                                json.decodeFromString(jsonString)
                            } catch (e: Exception) {
                                Logger.d("Could not deserialize $key 'jsonStr'", throwable = e)
                                null
                            },
                    )
                } ?: run {
                    Logger.d("Task Event $key missing 'jsonStr'")
                    null
                }

            "status.end" -> TaskEvent.End

            "status.geoip_lookup" ->
                TaskEvent.GeoIpLookup(
                    networkName = value?.probeNetworkName,
                    asn = value?.probeAsn,
                    ip = value?.probeIp,
                    countryCode = value?.probeCc,
                    networkType = networkTypeFinder(),
                )

            "status.measurement_done" ->
                TaskEvent.MeasurementDone(index = value?.idx ?: 0)

            "status.measurement_start" ->
                value?.input?.ifEmpty { null }?.let { url ->
                    TaskEvent.MeasurementStart(
                        index = value.idx,
                        url = url,
                    )
                } ?: run {
                    Logger.d("Task Event $key missing 'input'")
                    null
                }

            "status.measurement_submission" ->
                TaskEvent.MeasurementSubmissionSuccessful(index = value?.idx ?: 0)

            "status.progress" ->
                value?.percentage?.let { percentageValue ->
                    TaskEvent.Progress(
                        percentage = (percentageValue * 100.0).roundToInt(),
                        message = value.message,
                    )
                } ?: run {
                    Logger.d("Task Event $key missing 'percentage'")
                    null
                }

            "status.report_create" ->
                value?.reportId?.let {
                    TaskEvent.ReportCreate(reportId = it)
                } ?: run {
                    Logger.d("Task Event $key missing 'reportId'")
                    null
                }

            "status.started" -> TaskEvent.Started

            "task_terminated" -> TaskEvent.TaskTerminated

            else -> {
                Logger.d("Task Event $key ignored")
                null
            }
        }
    }
}
