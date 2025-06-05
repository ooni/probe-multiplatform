package org.ooni.probe.data.models

import org.ooni.engine.models.TaskOrigin

data class ResultFilter(
    val descriptors: List<Descriptor> = emptyList(),
    val taskOrigin: TaskOrigin? = null,
    val limit: Long = LIMIT,
) {
    val isAll get() = this == ResultFilter()

    companion object {
        const val LIMIT = 100L
    }
}
