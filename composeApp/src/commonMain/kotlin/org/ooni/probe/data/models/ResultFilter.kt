package org.ooni.probe.data.models

import org.ooni.engine.models.TaskOrigin

data class ResultFilter(
    val descriptor: Type<Descriptor> = Type.All,
    val taskOrigin: Type<TaskOrigin> = Type.All,
    val limit: Long = LIMIT,
) {
    val isAll get() = this == ResultFilter()

    sealed interface Type<out T> {
        data object All : Type<Nothing>

        data class One<T>(val value: T) : Type<T>
    }

    companion object {
        const val LIMIT = 100L
    }
}
