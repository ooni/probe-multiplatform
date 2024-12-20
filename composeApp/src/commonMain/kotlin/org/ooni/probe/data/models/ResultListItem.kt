package org.ooni.probe.data.models

data class ResultListItem(
    val result: ResultModel,
    val descriptor: Descriptor,
    val network: NetworkModel?,
    val measurementsCount: Long,
    val allMeasurementsUploaded: Boolean,
    val anyMeasurementUploadFailed: Boolean,
) {
    val idOrThrow get() = result.idOrThrow
}
