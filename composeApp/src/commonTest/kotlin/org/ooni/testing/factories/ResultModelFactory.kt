package org.ooni.testing.factories

import kotlinx.datetime.Instant
import org.ooni.probe.data.models.NetworkModel
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.TestDescriptorModel

object ResultModelFactory {
    fun build(
        id: ResultModel.Id? = null,
        testGroupName: String? = null,
        startTime: Instant? = null,
        isViewed: Boolean = false,
        isDone: Boolean = false,
        dataUsageUp: Long? = null,
        dataUsageDown: Long? = null,
        failureMessage: String? = null,
        networkId: NetworkModel.Id? = null,
        testDescriptorId: TestDescriptorModel.Id? = null,
    ) = ResultModel(
        id = id,
        testGroupName = testGroupName,
        startTime = startTime,
        isViewed = isViewed,
        isDone = isDone,
        dataUsageUp = dataUsageUp,
        dataUsageDown = dataUsageDown,
        failureMessage = failureMessage,
        networkId = networkId,
        testDescriptorId = testDescriptorId,
    )
}
