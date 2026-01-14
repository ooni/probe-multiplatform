package org.ooni.probe.data.models

import kotlinx.datetime.LocalDateTime
import org.ooni.engine.models.TaskOrigin
import kotlin.uuid.Uuid

data class RunModel(
    val id: Id,
    val network: NetworkModel?,
    val startTime: LocalDateTime,
    val taskOrigin: TaskOrigin,
) {
    data class Id(
        val value: String,
    ) {
        companion object {
            fun generateNew() = Id("internal_run_${Uuid.generateV4()}")

            fun generateForSingleResult(result: ResultModel) = Id("internal_run_result_${result.idOrThrow.value}")
        }
    }

    companion object {
        fun fromResult(item: ResultListItem) =
            RunModel(
                id = item.result.runId ?: Id.generateForSingleResult(item.result),
                network = item.network,
                startTime = item.result.startTime,
                taskOrigin = item.result.taskOrigin,
            )
    }
}
