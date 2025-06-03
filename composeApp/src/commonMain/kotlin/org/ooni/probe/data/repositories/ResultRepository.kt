package org.ooni.probe.data.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.Database
import org.ooni.probe.data.Network
import org.ooni.probe.data.Result
import org.ooni.probe.data.SelectAllWithNetwork
import org.ooni.probe.data.SelectByIdWithNetwork
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.MeasurementCounts
import org.ooni.probe.data.models.NetworkModel
import org.ooni.probe.data.models.ResultFilter
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.ResultWithNetworkAndAggregates
import org.ooni.probe.shared.toEpoch
import org.ooni.probe.shared.toLocalDateTime
import kotlin.coroutines.CoroutineContext

class ResultRepository(
    private val database: Database,
    private val backgroundContext: CoroutineContext,
) {
    fun list(filter: ResultFilter = ResultFilter()): Flow<List<ResultWithNetworkAndAggregates>> {
        val descriptorFilter = (filter.descriptor as? ResultFilter.Type.One)?.value
        val originFilter = (filter.taskOrigin as? ResultFilter.Type.One)?.value

        return database.resultQueries
            .selectAllWithNetwork(
                filterByDescriptor = if (descriptorFilter != null) 1 else 0,
                descriptorKey = descriptorFilter?.key,
                filterByTaskOrigin = if (originFilter != null) 1 else 0,
                taskOrigin = originFilter?.value,
                limit = filter.limit,
            )
            .asFlow()
            .mapToList(backgroundContext)
            .map { list -> list.mapNotNull { it.toModel() } }
    }

    fun getById(resultId: ResultModel.Id): Flow<Pair<ResultModel, NetworkModel?>?> =
        database.resultQueries
            .selectByIdWithNetwork(resultId.value)
            .asFlow()
            .mapToOneOrNull(backgroundContext)
            .map { it?.toModel() }

    fun getLatest(): Flow<ResultModel?> =
        database.resultQueries
            .selectLatest()
            .asFlow()
            .mapToOneOrNull(backgroundContext)
            .map { it?.toModel() }

    fun getLatestByDescriptor(descriptorKey: String): Flow<ResultModel?> =
        database.resultQueries
            .selectLatestByDescriptor(descriptorKey)
            .asFlow()
            .mapToOneOrNull(backgroundContext)
            .map { it?.toModel() }

    fun countMissingUpload(): Flow<Long> =
        database.resultQueries.countMissingUpload()
            .asFlow()
            .mapToOne(backgroundContext)

    suspend fun createOrUpdate(model: ResultModel): ResultModel.Id =
        withContext(backgroundContext) {
            database.transactionWithResult {
                database.resultQueries.insertOrReplace(
                    id = model.id?.value,
                    descriptor_name = model.descriptorName,
                    start_time = model.startTime.toEpoch(),
                    is_viewed = if (model.isViewed) 1 else 0,
                    is_done = if (model.isDone) 1 else 0,
                    data_usage_up = model.dataUsageUp,
                    data_usage_down = model.dataUsageDown,
                    failure_msg = model.failureMessage,
                    task_origin = model.taskOrigin.value,
                    network_id = model.networkId?.value,
                    descriptor_runId = model.descriptorKey?.id?.value,
                    descriptor_revision = model.descriptorKey?.revision,
                )
                model.id
                    ?: ResultModel.Id(
                        database.resultQueries.selectLastInsertedRowId().executeAsOne(),
                    )
            }
        }

    suspend fun getByIdAndUpdate(
        id: ResultModel.Id,
        update: (ResultModel) -> ResultModel,
    ) = withContext(backgroundContext) {
        getById(id).first()?.first?.let { result ->
            createOrUpdate(update(result))
        }
    }

    suspend fun markAsViewed(resultId: ResultModel.Id) =
        withContext(backgroundContext) {
            database.resultQueries.markAsViewed(resultId.value)
        }

    suspend fun markAllAsViewed() =
        withContext(backgroundContext) {
            database.resultQueries.markAllAsViewed()
        }

    fun countAllNotViewedFlow(): Flow<Long> =
        database.resultQueries.countAllNotViewed()
            .asFlow()
            .mapToOne(backgroundContext)

    suspend fun markAsDone(resultId: ResultModel.Id) =
        withContext(backgroundContext) {
            database.resultQueries.markAsDone(resultId.value)
        }

    suspend fun markAllAsDone() =
        withContext(backgroundContext) {
            database.resultQueries.markAllAsDone()
        }

    suspend fun deleteByRunId(descriptorId: InstalledTestDescriptorModel.Id) =
        withContext(backgroundContext) {
            database.resultQueries.deleteByRunId(descriptorId.value)
        }

    suspend fun deleteAll() {
        withContext(backgroundContext) {
            database.transaction {
                database.measurementQueries.deleteAll()
                database.resultQueries.deleteAll()
                database.networkQueries.deleteAll()
            }
        }
    }

    private fun Result.toModel(): ResultModel? {
        return ResultModel(
            id = ResultModel.Id(id),
            descriptorName = descriptor_name,
            startTime = start_time?.toLocalDateTime() ?: return null,
            isViewed = is_viewed == 1L,
            isDone = is_done == 1L,
            dataUsageUp = data_usage_up ?: 0L,
            dataUsageDown = data_usage_down ?: 0L,
            failureMessage = failure_msg,
            taskOrigin = TaskOrigin.fromValue(task_origin),
            networkId = network_id?.let(NetworkModel::Id),
            descriptorKey = descriptor_runId?.let {
                descriptor_revision?.let {
                    InstalledTestDescriptorModel.Key(
                        id = InstalledTestDescriptorModel.Id(descriptor_runId),
                        revision = descriptor_revision,
                    )
                }
            },
        )
    }

    private fun SelectAllWithNetwork.toModel(): ResultWithNetworkAndAggregates? {
        return ResultWithNetworkAndAggregates(
            result = Result(
                id = id ?: return null,
                descriptor_name = descriptor_name,
                start_time = start_time,
                is_viewed = is_viewed,
                is_done = is_done,
                data_usage_up = data_usage_up,
                data_usage_down = data_usage_down,
                failure_msg = failure_msg,
                task_origin = task_origin,
                network_id = network_id,
                descriptor_runId = descriptor_runId,
                descriptor_revision = descriptor_revision,
            ).toModel() ?: return null,
            network = network_id_inner?.let { networkId ->
                Network(
                    id = networkId,
                    network_name = network_name,
                    asn = asn,
                    country_code = country_code,
                    network_type = network_type,
                ).toModel()
            },
            measurementCounts = MeasurementCounts(
                done = doneMeasurementsCount ?: 0,
                failed = failedMeasurementsCount ?: 0,
                anomaly = anomalyMeasurementsCount ?: 0,
            ),
            allMeasurementsUploaded = allMeasurementsUploaded,
            anyMeasurementUploadFailed = anyMeasurementUploadFailed,
        )
    }

    private fun SelectByIdWithNetwork.toModel(): Pair<ResultModel, NetworkModel?>? {
        return Pair(
            Result(
                id = id,
                descriptor_name = descriptor_name,
                start_time = start_time,
                is_viewed = is_viewed,
                is_done = is_done,
                data_usage_up = data_usage_up,
                data_usage_down = data_usage_down,
                failure_msg = failure_msg,
                task_origin = task_origin,
                network_id = network_id,
                descriptor_runId = descriptor_runId,
                descriptor_revision = descriptor_revision,
            ).toModel() ?: return null,
            id_?.let { networkId ->
                Network(
                    id = networkId,
                    network_name = network_name,
                    asn = asn,
                    country_code = country_code,
                    network_type = network_type,
                ).toModel()
            },
        )
    }
}
