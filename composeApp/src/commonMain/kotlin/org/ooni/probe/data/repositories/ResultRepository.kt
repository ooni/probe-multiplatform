package org.ooni.probe.data.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import org.ooni.probe.Database
import org.ooni.probe.data.Result
import org.ooni.probe.data.models.NetworkModel
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.TestDescriptorModel

class ResultRepository(
    private val database: Database,
    private val backgroundDispatcher: CoroutineDispatcher,
) {
    fun list() =
        database.resultQueries
            .selectAll()
            .asFlow()
            .mapToList(backgroundDispatcher)
            .map { list -> list.map { it.toModel() } }

    suspend fun create(model: ResultModel) {
        withContext(backgroundDispatcher) {
            database.resultQueries.insert(
                id = model.id?.value,
                test_group_name = model.testGroupName,
                start_time = model.startTime?.toEpochMilliseconds(),
                is_viewed = if (model.isViewed) 1 else 0,
                is_done = if (model.isDone) 1 else 0,
                data_usage_up = model.dataUsageUp,
                data_usage_down = model.dataUsageDown,
                failure_msg = model.failureMessage,
                network_id = model.networkId?.value,
                descriptor_runId = model.testDescriptorId?.value,
            )
        }
    }

    private fun Result.toModel(): ResultModel =
        ResultModel(
            id = ResultModel.Id(id),
            testGroupName = test_group_name,
            startTime = start_time?.let(Instant::fromEpochMilliseconds),
            isViewed = is_viewed == 1L,
            isDone = is_done == 1L,
            dataUsageUp = data_usage_up,
            dataUsageDown = data_usage_down,
            failureMessage = failure_msg,
            networkId = network_id?.let(NetworkModel::Id),
            testDescriptorId = descriptor_runId?.let(TestDescriptorModel::Id),
        )
}
