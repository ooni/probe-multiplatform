package org.ooni.testing.factories

import kotlinx.datetime.Instant
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.UrlModel

object MeasurementModelFactory {
    fun build(
        id: MeasurementModel.Id? = null,
        testName: String? = null,
        startTime: Instant? = null,
        runtime: Double? = null,
        isDone: Boolean = false,
        isUploaded: Boolean = false,
        isFailed: Boolean = false,
        failureMessage: String? = null,
        isUploadFailed: Boolean = false,
        uploadFailureMessage: String? = null,
        isRerun: Boolean = false,
        isAnomaly: Boolean = false,
        reportId: String? = null,
        testKeys: String? = null,
        rerunNetwork: String? = null,
        urlId: UrlModel.Id? = null,
        resultId: ResultModel.Id? = null,
    ) = MeasurementModel(
        id = id,
        testName = testName,
        startTime = startTime,
        runtime = runtime,
        isDone = isDone,
        isUploaded = isUploaded,
        isFailed = isFailed,
        failureMessage = failureMessage,
        isUploadFailed = isUploadFailed,
        uploadFailureMessage = uploadFailureMessage,
        isRerun = isRerun,
        isAnomaly = isAnomaly,
        reportId = reportId,
        testKeys = testKeys,
        rerunNetwork = rerunNetwork,
        urlId = urlId,
        resultId = resultId,
    )
}
