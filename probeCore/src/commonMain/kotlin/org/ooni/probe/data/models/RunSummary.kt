package org.ooni.probe.data.models

data class RunSummary(
    val run: RunModel,
    val results: List<ResultWithNetworkAndAggregates>,
) {
    val measurementCounts
        get() = MeasurementCounts(
            done = results.sumOf { it.measurementCounts.done },
            failed = results.sumOf { it.measurementCounts.failed },
            anomaly = results.sumOf { it.measurementCounts.anomaly },
        )

    companion object {
        fun fromResults(results: List<ResultWithNetworkAndAggregates>) =
            results.firstOrNull()?.let { firstResult ->
                RunSummary(
                    run = RunModel.fromResultAndNetwork(firstResult.result, firstResult.network),
                    results = results,
                )
            }
    }
}
