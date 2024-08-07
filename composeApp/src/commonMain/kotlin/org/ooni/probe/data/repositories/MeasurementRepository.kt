package org.ooni.probe.data.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import org.ooni.probe.Database
import org.ooni.probe.data.Measurement
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.UrlModel

class MeasurementRepository(
    private val database: Database,
    private val backgroundDispatcher: CoroutineDispatcher,
) {
    fun list() =
        database.measurementQueries
            .selectAll()
            .asFlow()
            .mapToList(backgroundDispatcher)
            .map { list -> list.map { it.toModel() } }

    suspend fun create(model: MeasurementModel) {
        withContext(backgroundDispatcher) {
            database.measurementQueries.insert(
                id = model.id?.value,
                test_name = model.testName,
                start_time = model.startTime?.toEpochMilliseconds(),
                runtime = model.runtime,
                is_done = if (model.isDone) 1 else 0,
                is_uploaded = if (model.isUploaded) 1 else 0,
                is_failed = if (model.isFailed) 1 else 0,
                failure_msg = model.failureMessage,
                is_upload_failed = if (model.isUploadFailed) 1 else 0,
                upload_failure_msg = model.uploadFailureMessage,
                is_rerun = if (model.isRerun) 1 else 0,
                is_anomaly = if (model.isAnomaly) 1 else 0,
                report_id = model.reportId,
                test_keys = model.testKeys,
                rerun_network = model.rerunNetwork,
                url_id = model.urlId?.value,
                result_id = model.resultId?.value,
            )
        }
    }

    private fun Measurement.toModel(): MeasurementModel =
        MeasurementModel(
            id = MeasurementModel.Id(id),
            testName = test_name,
            startTime = start_time?.let(Instant::fromEpochMilliseconds),
            runtime = runtime,
            isDone = is_done == 1L,
            isUploaded = is_uploaded == 1L,
            isFailed = is_failed == 1L,
            failureMessage = failure_msg,
            isUploadFailed = is_upload_failed == 1L,
            uploadFailureMessage = upload_failure_msg,
            isRerun = is_rerun == 1L,
            isAnomaly = is_anomaly == 1L,
            reportId = report_id,
            testKeys = test_keys,
            rerunNetwork = rerun_network,
            urlId = url_id?.let(UrlModel::Id),
            resultId = result_id?.let(ResultModel::Id),
        )
}
