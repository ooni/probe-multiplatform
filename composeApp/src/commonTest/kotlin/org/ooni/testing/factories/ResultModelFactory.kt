package org.ooni.testing.factories

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.MeasurementCounts
import org.ooni.probe.data.models.NetworkModel
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.ResultWithNetworkAndAggregates
import org.ooni.probe.shared.now
import org.ooni.probe.shared.today
import kotlin.random.Random

object ResultModelFactory {
    fun build(
        id: ResultModel.Id? = ResultModel.Id(Random.nextLong()),
        descriptorName: String? = "websites",
        startTime: LocalDateTime = LocalDateTime.nowWithoutNanoseconds(),
        isViewed: Boolean = false,
        isDone: Boolean = false,
        dataUsageUp: Long = 0,
        dataUsageDown: Long = 0,
        failureMessage: String? = null,
        taskOrigin: TaskOrigin = TaskOrigin.OoniRun,
        networkId: NetworkModel.Id? = null,
        descriptorKey: InstalledTestDescriptorModel.Key? = null,
    ) = ResultModel(
        id = id,
        descriptorName = descriptorName,
        startTime = startTime,
        isViewed = isViewed,
        isDone = isDone,
        dataUsageUp = dataUsageUp,
        dataUsageDown = dataUsageDown,
        failureMessage = failureMessage,
        taskOrigin = taskOrigin,
        networkId = networkId,
        descriptorKey = descriptorKey,
    )

}

private fun LocalDateTime.Companion.nowWithoutNanoseconds(): LocalDateTime {
    val now = LocalDateTime.now()
    return LocalDate.today().atTime(now.hour, now.minute, now.second)
}
