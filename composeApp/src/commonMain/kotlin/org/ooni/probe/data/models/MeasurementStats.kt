package org.ooni.probe.data.models

data class MeasurementStats(
    val measurementsToday: Long,
    val measurementsWeek: Long,
    val measurementsMonth: Long,
    val networks: Long,
    val countries: Long,
) {
    val isEmpty get() = networks == 0L
}
