package org.ooni.probe.data.models

data class ResultListItem(
    val result: ResultModel,
    val descriptor: Descriptor,
    val network: NetworkModel?,
    val doneMeasurementsCount: Long,
    val failedMeasurementsCount: Long,
    val anomalyMeasurementsCount: Long,
    val allMeasurementsUploaded: Boolean,
    val anyMeasurementUploadFailed: Boolean,
) {
    val idOrThrow
        get() = result.idOrThrow
    val successMeasurementsCount
        get() = doneMeasurementsCount - failedMeasurementsCount - anomalyMeasurementsCount
    val testedMeasurementsCount
        get() = doneMeasurementsCount - failedMeasurementsCount
}
