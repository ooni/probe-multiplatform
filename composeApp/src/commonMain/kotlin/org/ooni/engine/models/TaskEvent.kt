package org.ooni.engine.models

sealed interface TaskEvent {
    data class BugJsonDump(
        val value: TaskEventResult.Value,
    ) : TaskEvent

    data object End : TaskEvent

    data class GeoIpLookup(
        val networkName: String?,
        val ip: String?,
        val asn: String?,
        val countryCode: String?,
        val networkType: NetworkType,
    ) : TaskEvent

    data class Log(
        val level: String?,
        val message: String,
    ) : TaskEvent

    data class Measurement(
        val index: Int,
        val json: String,
        val result: MeasurementResult?,
    ) : TaskEvent

    data class MeasurementDone(
        val index: Int,
    ) : TaskEvent

    data class MeasurementStart(
        val index: Int,
        val url: String,
    ) : TaskEvent

    data class MeasurementSubmissionSuccessful(
        val index: Int,
    ) : TaskEvent

    data class MeasurementSubmissionFailure(
        val index: Int,
        val message: String?,
    ) : TaskEvent

    data class Progress(
        val percentage: Int,
        val message: String?,
    ) : TaskEvent

    data class ReportCreate(
        val reportId: String,
    ) : TaskEvent

    data class ResolverLookupFailure(
        val message: String?,
    ) : TaskEvent

    data object Started : TaskEvent

    data class StartupFailure(
        val message: String?,
    ) : TaskEvent

    data object TaskTerminated : TaskEvent
}
