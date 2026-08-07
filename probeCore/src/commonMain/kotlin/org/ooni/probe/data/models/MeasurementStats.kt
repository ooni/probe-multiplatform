package org.ooni.probe.data.models

data class MeasurementStats(
    val measurementsToday: Long,
    val measurementsWeek: Long,
    val measurementsMonth: Long,
    val measurementsTotal: Long,
    val networks: Long,
    val countries: List<String>,
)
