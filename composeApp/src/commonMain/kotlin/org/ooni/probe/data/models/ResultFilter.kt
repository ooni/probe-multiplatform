package org.ooni.probe.data.models

import org.ooni.engine.models.TaskOrigin

data class ResultFilter(
    val descriptor: Type<Descriptor> = Type.All,
    val taskOrigin: Type<TaskOrigin> = Type.All,
) {
    val isAll get() = this == ResultFilter()

    sealed interface Type<out T> {
        data object All : Type<Nothing>

        data class One<T>(val value: T) : Type<T>
    }
}
