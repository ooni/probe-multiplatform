package org.ooni.probe.data.models

import kotlinx.datetime.LocalDateTime

data class ResultModel(
    val id: Id? = null,
    val testGroupName: String?,
    val startTime: LocalDateTime,
    val isViewed: Boolean,
    val isDone: Boolean,
    val dataUsageUp: Long?,
    val dataUsageDown: Long?,
    val failureMessage: String?,
    val networkId: NetworkModel.Id?,
    val testDescriptorId: InstalledTestDescriptorModel.Id?,
) {
    data class Id(
        val value: Long,
    )

    val idOrThrow get() = id ?: throw IllegalStateException("Id no available")
}
