package org.ooni.probe.data.models

import kotlinx.datetime.Instant

data class ResultModel(
    val id: Id? = null,
    val testGroupName: String?,
    val startTime: Instant?,
    val isViewed: Boolean,
    val isDone: Boolean,
    val dataUsageUp: Long?,
    val dataUsageDown: Long?,
    val failureMessage: String?,
    val networkId: NetworkModel.Id?,
    val testDescriptorId: TestDescriptorModel.Id?,
) {
    data class Id(
        val value: Long,
    )
}
