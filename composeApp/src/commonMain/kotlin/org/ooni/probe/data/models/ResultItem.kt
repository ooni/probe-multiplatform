package org.ooni.probe.data.models

data class ResultItem(
    val result: ResultModel,
    val descriptor: Descriptor,
    val network: NetworkModel?,
    val measurements: List<MeasurementWithUrl>,
) {
    val anyMeasurementMissingUpload = measurements.any { it.measurement.isMissingUpload }
}
