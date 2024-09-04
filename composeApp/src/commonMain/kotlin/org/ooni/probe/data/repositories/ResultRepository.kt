package org.ooni.probe.data.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.Database
import org.ooni.probe.data.Network
import org.ooni.probe.data.Result
import org.ooni.probe.data.SelectAllWithNetwork
import org.ooni.probe.data.SelectByIdWithNetwork
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.NetworkModel
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.ResultWithNetworkAndAggregates
import org.ooni.probe.shared.toEpoch
import org.ooni.probe.shared.toLocalDateTime

class ResultRepository(
    private val database: Database,
    private val backgroundDispatcher: CoroutineDispatcher,
) {
    fun list(): Flow<List<ResultModel>> =
        database.resultQueries
            .selectAll()
            .asFlow()
            .mapToList(backgroundDispatcher)
            .map { list -> list.mapNotNull { it.toModel() } }

    fun listWithNetwork(): Flow<List<ResultWithNetworkAndAggregates>> =
        database.resultQueries
            .selectAllWithNetwork()
            .asFlow()
            .mapToList(backgroundDispatcher)
            .map { list -> list.mapNotNull { it.toModel() } }

    fun getById(resultId: ResultModel.Id): Flow<Pair<ResultModel, NetworkModel?>?> =
        database.resultQueries
            .selectByIdWithNetwork(resultId.value)
            .asFlow()
            .mapToOneOrNull(backgroundDispatcher)
            .map { it?.toModel() }

    fun getLatest(): Flow<ResultModel?> =
        database.resultQueries
            .selectLatest()
            .asFlow()
            .mapToOneOrNull(backgroundDispatcher)
            .map { it?.toModel() }

    suspend fun createOrUpdate(model: ResultModel): ResultModel.Id =
        withContext(backgroundDispatcher) {
            database.transactionWithResult {
                database.resultQueries.insertOrReplace(
                    id = model.id?.value,
                    test_group_name = model.testGroupName,
                    start_time = model.startTime.toEpoch(),
                    is_viewed = if (model.isViewed) 1 else 0,
                    is_done = if (model.isDone) 1 else 0,
                    data_usage_up = model.dataUsageUp,
                    data_usage_down = model.dataUsageDown,
                    failure_msg = model.failureMessage,
                    task_origin = model.taskOrigin.value,
                    network_id = model.networkId?.value,
                    descriptor_runId = model.testDescriptorId?.value,
                )
                model.id
                    ?: ResultModel.Id(
                        database.resultQueries.selectLastInsertedRowId().executeAsOne(),
                    )
            }
        }

    suspend fun markAsViewed(resultId: ResultModel.Id) =
        withContext(backgroundDispatcher) {
            database.resultQueries.markAsViewed(resultId.value)
        }

    private fun Result.toModel(): ResultModel? {
        return ResultModel(
            id = ResultModel.Id(id),
            testGroupName = test_group_name,
            startTime = start_time?.toLocalDateTime() ?: return null,
            isViewed = is_viewed == 1L,
            isDone = is_done == 1L,
            dataUsageUp = data_usage_up ?: 0L,
            dataUsageDown = data_usage_down ?: 0L,
            failureMessage = failure_msg,
            taskOrigin = TaskOrigin.fromValue(task_origin),
            networkId = network_id?.let(NetworkModel::Id),
            testDescriptorId = descriptor_runId?.let(InstalledTestDescriptorModel::Id),
        )
    }

    private fun SelectAllWithNetwork.toModel(): ResultWithNetworkAndAggregates? {
        return ResultWithNetworkAndAggregates(
            result = Result(
                id = id,
                test_group_name = test_group_name,
                start_time = start_time,
                is_viewed = is_viewed,
                is_done = is_done,
                data_usage_up = data_usage_up,
                data_usage_down = data_usage_down,
                failure_msg = failure_msg,
                task_origin = task_origin,
                network_id = network_id,
                descriptor_runId = descriptor_runId,
            ).toModel() ?: return null,
            network = id_?.let { networkId ->
                Network(
                    id = networkId,
                    network_name = network_name,
                    ip = ip,
                    asn = asn,
                    country_code = country_code,
                    network_type = network_type,
                ).toModel()
            },
            measurementsCount = measurementsCount,
            allMeasurementsUploaded = allMeasurementsUploaded,
        )
    }

    private fun SelectByIdWithNetwork.toModel(): Pair<ResultModel, NetworkModel?>? {
        return Pair(
            Result(
                id = id,
                test_group_name = test_group_name,
                start_time = start_time,
                is_viewed = is_viewed,
                is_done = is_done,
                data_usage_up = data_usage_up,
                data_usage_down = data_usage_down,
                failure_msg = failure_msg,
                task_origin = task_origin,
                network_id = network_id,
                descriptor_runId = descriptor_runId,
            ).toModel() ?: return null,
            id_?.let { networkId ->
                Network(
                    id = networkId,
                    network_name = network_name,
                    ip = ip,
                    asn = asn,
                    country_code = country_code,
                    network_type = network_type,
                ).toModel()
            },
        )
    }
}
