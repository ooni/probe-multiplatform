package org.ooni.probe.data.models

data class Run(
    val results: List<ResultWithNetworkAndAggregates>,
) {
    val startTime get() = results.first().result.startTime

    val measurementCounts = MeasurementCounts(
        done = results.sumOf { it.measurementCounts.done },
        failed = results.sumOf { it.measurementCounts.failed },
        anomaly = results.sumOf { it.measurementCounts.anomaly },
    )
}
