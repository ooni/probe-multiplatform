package org.ooni.engine

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import org.ooni.engine.models.TaskEvent
import org.ooni.engine.models.TaskEventResult

class TaskEventMapper(
    private val networkTypeFinder: NetworkTypeFinder,
    private val json: Json,
) {
    operator fun invoke(
        result: TaskEventResult,
        isCancelled: Boolean = false,
    ): TaskEvent? {
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

            "failure.resolver_lookup" ->
                value?.let {
                    TaskEvent.ResolverLookupFailure(
                        message = value.failure,
                        value = value,
                        isCancelled = isCancelled,
                    )
                } ?: run {
                    Logger.d("Task Event $key missing 'value'")
                    null
                }

            "failure.startup" ->
                value?.let {
                    TaskEvent.StartupFailure(
                        message = value.failure,
                        value = value,
                        isCancelled = isCancelled,
                    )
                } ?: run {
                    Logger.d("Task Event $key missing 'value'")
                    null
                }

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

            "status.end" ->
                TaskEvent.End(
                    downloadedKb = value?.downloadedKb?.toLong() ?: 0L,
                    uploadedKb = value?.uploadedKb?.toLong() ?: 0L,
                )

            "status.geoip_lookup" ->
                TaskEvent.GeoIpLookup(
                    networkName = value?.probeNetworkName,
                    asn = value?.probeAsn,
                    ip = value?.probeIp,
                    countryCode = value?.probeCc,
                    geoIpdb = value?.geoIpdb,
                    networkType = networkTypeFinder(),
                )

            "status.resolver_lookup" -> value?.geoIpdb?.let {
                println(it)
                null
            }

            "status.measurement_done" ->
                TaskEvent.MeasurementDone(index = value?.idx ?: 0)

            "status.measurement_start" ->
                TaskEvent.MeasurementStart(
                    index = value?.idx ?: 0,
                    url = value?.input,
                )

            "status.measurement_submission" ->
                TaskEvent.MeasurementSubmissionSuccessful(
                    index = value?.idx ?: 0,
                    measurementUid = value?.measurementUid,
                )

            "status.progress" ->
                value?.percentage?.let { progress ->
                    TaskEvent.Progress(
                        progress = progress,
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

            "task_terminated" ->
                TaskEvent.TaskTerminated(
                    index = value?.idx ?: 0,
                )

            else -> {
                Logger.d("Task Event $key ignored")
                null
            }
        }
    }
}
