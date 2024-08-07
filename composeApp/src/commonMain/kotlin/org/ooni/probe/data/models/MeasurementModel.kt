package org.ooni.probe.data.models

import kotlinx.datetime.Instant

data class MeasurementModel(
    val id: Id? = null,
    val testName: String?,
    val startTime: Instant?,
    val runtime: Double?,
    val isDone: Boolean,
    val isUploaded: Boolean,
    val isFailed: Boolean,
    val failureMessage: String?,
    val isUploadFailed: Boolean,
    val uploadFailureMessage: String?,
    val isRerun: Boolean,
    val isAnomaly: Boolean,
    val reportId: String?,
    val testKeys: String?,
    val rerunNetwork: String?,
    val urlId: UrlModel.Id?,
    val resultId: ResultModel.Id?,
) {
    data class Id(
        val value: Long,
    )
}
