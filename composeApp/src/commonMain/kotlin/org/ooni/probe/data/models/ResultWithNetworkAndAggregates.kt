package org.ooni.probe.data.models

data class ResultWithNetworkAndAggregates(
    val result: ResultModel,
    val network: NetworkModel?,
    val doneMeasurementsCount: Long,
    val failedMeasurementsCount: Long,
    val anomalyMeasurementsCount: Long,
    val allMeasurementsUploaded: Boolean,
    val anyMeasurementUploadFailed: Boolean,
)
