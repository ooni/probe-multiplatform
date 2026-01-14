package org.ooni.probe.data.models

data class MeasurementCounts(
    val done: Long = 0L,
    val failed: Long = 0L,
    val anomaly: Long = 0L,
) {
    val success get() = done - failed - anomaly
    val tested get() = done - failed

    fun add(counts: MeasurementCounts) = copy(
        done = done + counts.done,
        failed = failed + counts.failed,
        anomaly = anomaly + counts.anomaly,
    )
}
