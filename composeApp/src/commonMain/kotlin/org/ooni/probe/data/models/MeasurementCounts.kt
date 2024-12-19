package org.ooni.probe.data.models

data class MeasurementCounts(
    val done: Long,
    val failed: Long,
    val anomaly: Long,
) {
    val success get() = done - failed - anomaly
    val tested get() = done - failed
}
