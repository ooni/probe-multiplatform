package org.ooni.probe.data.models

data class ResultWithNetworkAndAggregates(
    val result: ResultModel,
    val network: NetworkModel?,
    val measurementCounts: MeasurementCounts,
    val allMeasurementsUploaded: Boolean,
    val anyMeasurementUploadFailed: Boolean,
)
