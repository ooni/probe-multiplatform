package org.ooni.engine.models

sealed interface TaskEvent {
    data class BugJsonDump(
        val value: TaskEventResult.Value,
    ) : TaskEvent

    data class End(
        val downloadedKb: Long,
        val uploadedKb: Long,
    ) : TaskEvent

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
        val url: String?,
    ) : TaskEvent

    data class MeasurementSubmissionSuccessful(
        val index: Int,
        val measurementUid: String?,
    ) : TaskEvent

    data class MeasurementSubmissionFailure(
        val index: Int,
        val message: String?,
    ) : TaskEvent

    data class Progress(
        val progress: Double,
        val message: String?,
    ) : TaskEvent

    data class ReportCreate(
        val reportId: String,
    ) : TaskEvent

    data class ResolverLookupFailure(
        val message: String?,
        val value: TaskEventResult.Value,
        val isCancelled: Boolean,
    ) : TaskEvent

    data object Started : TaskEvent

    data class StartupFailure(
        val message: String?,
        val value: TaskEventResult.Value,
        val isCancelled: Boolean,
    ) : TaskEvent

    data class TaskTerminated(
        val index: Int,
    ) : TaskEvent

    fun isCancelled() =
        when (this) {
            is StartupFailure -> isCancelled
            is ResolverLookupFailure -> isCancelled
            else -> null
        }
}
