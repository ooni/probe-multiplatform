package org.ooni.probe.data.models

data class ResultWithNetworkAndAggregates(
    val result: ResultModel,
    val network: NetworkModel?,
    val measurementsCount: Long,
    val allMeasurementsUploaded: Boolean,
    val anyMeasurementUploadFailed: Boolean,
)
