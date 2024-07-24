package org.ooni.engine

sealed interface TaskEvent {
    data class Log(
        val level: String?,
        val message: String
    ): TaskEvent

    data object Started : TaskEvent

    data class ReportCreate(
        val reportId: String
    ) : TaskEvent

    data class Progress(
        val percentage: Int,
        val message: String?
    ): TaskEvent

    data object StatusEnd : TaskEvent

    data object TaskTerminated : TaskEvent

    data class FailureStartup(
        val message: String?
    ): TaskEvent
}