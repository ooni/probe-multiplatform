package org.ooni.probe.data.models

import kotlinx.datetime.LocalDateTime
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.shared.now

data class ResultModel(
    val id: Id? = null,
    val startTime: LocalDateTime = LocalDateTime.now(),
    val isViewed: Boolean = false,
    val isDone: Boolean = false,
    val dataUsageUp: Long = 0,
    val dataUsageDown: Long = 0,
    val failureMessage: String? = null,
    val taskOrigin: TaskOrigin,
    val networkId: NetworkModel.Id? = null,
    val descriptorName: String?,
    val descriptorKey: InstalledTestDescriptorModel.Key?,
) {
    data class Id(
        val value: Long,
    )

    val idOrThrow get() = id ?: throw IllegalStateException("Id no available")
}
