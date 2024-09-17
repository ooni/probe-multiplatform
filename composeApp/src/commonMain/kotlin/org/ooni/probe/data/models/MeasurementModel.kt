package org.ooni.probe.data.models

import kotlinx.datetime.LocalDateTime
import okio.Path
import okio.Path.Companion.toPath
import org.ooni.engine.models.TestType
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
    val testKeys: String? = null,
    val rerunNetwork: String? = null,
    val urlId: UrlModel.Id?,
    val resultId: ResultModel.Id,
) {
    data class Id(
        val value: Long,
    )

    data class ReportId(
        val value: String,
    )

    val idOrThrow get() = id ?: throw IllegalStateException("Id no available")

    val logFilePath: Path
        get() = logFilePath(resultId, test)

    val reportFilePath: Path?
        get() = id?.let { "Measurement/${id.value}_${test.name}.json".toPath() }

    val isMissingUpload
        get() = isDone && (!isUploaded || reportId == null)

    companion object {
        fun logFilePath(
            resultId: ResultModel.Id,
            test: TestType,
        ): Path = "Measurement/${resultId.value}_${test.name}.log".toPath()
    }
}
