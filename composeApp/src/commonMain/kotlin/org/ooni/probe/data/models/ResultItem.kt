package org.ooni.probe.data.models

import kotlin.time.Duration.Companion.seconds

data class ResultCount(val total: Long, val succeeded: Long, val anomaly: Long)

data class ResultItem(
    val result: ResultModel,
    val descriptor: Descriptor,
    val network: NetworkModel?,
    val measurements: List<MeasurementWithUrl>,
    val testKeys: List<TestKeysWithResultId>?,
) {
    val anyMeasurementMissingUpload =
        result.isDone && measurements.any { it.measurement.isDoneAndMissingUpload }

    val totalRuntime get() = measurements.sumOf { it.measurement.runtime ?: 0.0 }.seconds

    val canBeRerun get() = descriptor.name == "websites" && result.isDone && urlCount > 0

    val urlCount get() = measurements.count { it.url != null }

    val measurementCounts
        get() = ResultCount(
            total = measurements.size.toLong(),
            succeeded = measurements.count {
                !it.measurement.isFailed && !it.measurement.isAnomaly && it.measurement.isDone
            }.toLong(),
            anomaly = measurements.count { it.measurement.isAnomaly }.toLong(),
        )
}
