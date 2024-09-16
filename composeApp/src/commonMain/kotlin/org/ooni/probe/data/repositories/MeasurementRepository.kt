package org.ooni.probe.data.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.ooni.engine.models.TestType
import org.ooni.probe.Database
import org.ooni.probe.data.Measurement
import org.ooni.probe.data.SelectByResultIdWithUrl
import org.ooni.probe.data.Url
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementWithUrl
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.UrlModel
import org.ooni.probe.shared.toEpoch
import org.ooni.probe.shared.toLocalDateTime

class MeasurementRepository(
    private val database: Database,
    private val backgroundDispatcher: CoroutineDispatcher,
) {
    fun list(): Flow<List<MeasurementModel>> =
        database.measurementQueries
            .selectAll()
            .asFlow()
            .mapToList(backgroundDispatcher)
            .map { list -> list.mapNotNull { it.toModel() } }

    fun listByResultId(id: ResultModel.Id) =
        database.measurementQueries
            .selectByResultIdWithUrl(id.value)
            .asFlow()
            .mapToList(backgroundDispatcher)
            .map { list -> list.mapNotNull { it.toModel() } }

    fun listNotUploaded() =
        database.measurementQueries
            .selectAllNotUploaded()
            .asFlow()
            .mapToList(backgroundDispatcher)
            .map { list -> list.mapNotNull { it.toModel() } }

    suspend fun createOrUpdate(model: MeasurementModel): MeasurementModel.Id =
        withContext(backgroundDispatcher) {
            database.transactionWithResult {
                database.measurementQueries.insertOrReplace(
                    id = model.id?.value,
                    test_name = model.test.name,
                    start_time = model.startTime?.toEpoch(),
                    runtime = model.runtime,
                    is_done = if (model.isDone) 1 else 0,
                    is_uploaded = if (model.isUploaded) 1 else 0,
                    is_failed = if (model.isFailed) 1 else 0,
                    failure_msg = model.failureMessage,
                    is_upload_failed = if (model.isUploadFailed) 1 else 0,
                    upload_failure_msg = model.uploadFailureMessage,
                    is_rerun = if (model.isRerun) 1 else 0,
                    is_anomaly = if (model.isAnomaly) 1 else 0,
                    report_id = model.reportId?.value,
                    test_keys = model.testKeys,
                    rerun_network = model.rerunNetwork,
                    url_id = model.urlId?.value,
                    result_id = model.resultId.value,
                )

                model.id ?: MeasurementModel.Id(
                    database.measurementQueries.selectLastInsertedRowId().executeAsOne(),
                )
            }
        }

    suspend fun deleteByResultRunId(descriptorId: InstalledTestDescriptorModel.Id) {
        withContext(backgroundDispatcher) {
            database.measurementQueries.deleteByResultRunId(descriptorId.value)
        }
    }

    fun selectByResultRunId(descriptorId: InstalledTestDescriptorModel.Id) =
        database.measurementQueries
            .selectByResultRunId(descriptorId.value)
            .asFlow()
            .mapToList(backgroundDispatcher)
            .map { list -> list.mapNotNull { it.toModel() } }

    private fun Measurement.toModel(): MeasurementModel? {
        return MeasurementModel(
            id = MeasurementModel.Id(id),
            test = test_name?.let(TestType::fromName) ?: return null,
            startTime = start_time?.toLocalDateTime(),
            runtime = runtime,
            isDone = is_done == 1L,
            isUploaded = is_uploaded == 1L,
            isFailed = is_failed == 1L,
            failureMessage = failure_msg,
            isUploadFailed = is_upload_failed == 1L,
            uploadFailureMessage = upload_failure_msg,
            isRerun = is_rerun == 1L,
            isAnomaly = is_anomaly == 1L,
            reportId = report_id?.let(MeasurementModel::ReportId),
            testKeys = test_keys,
            rerunNetwork = rerun_network,
            urlId = url_id?.let(UrlModel::Id),
            resultId = result_id?.let(ResultModel::Id) ?: return null,
        )
    }

    private fun SelectByResultIdWithUrl.toModel(): MeasurementWithUrl? {
        return MeasurementWithUrl(
            measurement = Measurement(
                id = id,
                test_name = test_name,
                start_time = start_time,
                runtime = runtime,
                is_done = is_done,
                is_uploaded = is_uploaded,
                is_failed = is_failed,
                failure_msg = failure_msg,
                is_upload_failed = is_upload_failed,
                upload_failure_msg = upload_failure_msg,
                is_rerun = is_rerun,
                is_anomaly = is_anomaly,
                report_id = report_id,
                test_keys = test_keys,
                rerun_network = rerun_network,
                url_id = url_id,
                result_id = result_id,
            ).toModel() ?: return null,
            url = id_?.let { urlId ->
                Url(
                    id = urlId,
                    url = url,
                    country_code = country_code,
                    category_code = category_code,
                ).toModel()
            },
        )
    }
}
