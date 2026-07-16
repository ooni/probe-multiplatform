package org.ooni.probe.data.models

import kotlinx.datetime.LocalDateTime
import okio.Path
import okio.Path.Companion.toPath
import org.ooni.engine.models.TestType
import org.ooni.passport.models.VerificationStatus
import org.ooni.probe.shared.now

data class MeasurementModel(
    val id: Id? = null,
    val test: TestType,
    val startTime: LocalDateTime? = LocalDateTime.now(),
    val runtime: Double? = null,
    val isDone: Boolean = false,
    val isUploaded: Boolean = false,
    val isFailed: Boolean = false,
    val failureMessage: String? = null,
    val isUploadFailed: Boolean = false,
    val uploadFailureMessage: String? = null,
    val isRerun: Boolean = false,
    val isAnomaly: Boolean = false,
    val reportId: ReportId?,
    val uid: Uid? = null,
    val testKeys: String? = null,
    val rerunNetwork: String? = null,
    val verificationStatus: VerificationStatus? = null,
    val urlId: UrlModel.Id?,
    val resultId: ResultModel.Id,
) {
    data class Id(
        val value: Long,
    )

    data class ReportId(
        val value: String,
    )

    data class Uid(
        val value: String,
    )

    val idOrThrow get() = id ?: throw IllegalStateException("Id no available")

    val logFilePath: Path
        get() = logFilePath(resultId, test)

    val reportFilePath: Path?
        get() = id?.let { "Measurement/${id.value}_${test.name}.json".toPath() }

    val isMissingUpload
        get() = !isUploaded

    val isDoneAndMissingUpload
        get() = isDone && isMissingUpload

    /**
     * Derived (not persisted): the report file could not be parsed and can never be submitted.
     * Reuses the already-persisted [uploadFailureMessage] as a durable marker so this survives a
     * DB round-trip without a schema change. Only [org.ooni.probe.domain.SubmitMeasurement] writes
     * that field, and transient network/HTTP messages never start with [REPORT_UNPARSEABLE_PREFIX].
     */
    val isUploadFailedPermanently: Boolean
        get() = isUploadFailed && uploadFailureMessage?.startsWith(REPORT_UNPARSEABLE_PREFIX) == true

    companion object {
        const val REPORT_UNPARSEABLE_PREFIX = "Report unparseable:"

        fun logFilePath(
            resultId: ResultModel.Id,
            test: TestType,
        ): Path = "Measurement/${resultId.value}_${test.name}.log".toPath()
    }
}
