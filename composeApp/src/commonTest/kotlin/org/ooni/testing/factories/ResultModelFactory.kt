package org.ooni.testing.factories

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.NetworkModel
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.shared.today

object ResultModelFactory {
    fun build(
        id: ResultModel.Id? = ResultModel.Id(1234L),
        descriptorName: String? = "websites",
        startTime: LocalDateTime = LocalDate.today().atTime(0, 0),
        isViewed: Boolean = false,
        isDone: Boolean = false,
        dataUsageUp: Long = 0,
        dataUsageDown: Long = 0,
        failureMessage: String? = null,
        taskOrigin: TaskOrigin = TaskOrigin.OoniRun,
        networkId: NetworkModel.Id? = null,
        descriptorKey: Descriptor.Key? = null,
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
