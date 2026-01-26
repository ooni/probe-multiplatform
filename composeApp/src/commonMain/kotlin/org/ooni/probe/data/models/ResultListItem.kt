package org.ooni.probe.data.models

data class ResultListItem(
    val result: ResultModel,
    val descriptor: DescriptorItem,
    val network: NetworkModel?,
    val measurementCounts: MeasurementCounts,
    val allMeasurementsUploaded: Boolean,
    val anyMeasurementUploadFailed: Boolean,
    val testKeys: List<TestKeysWithResultId>?,
) {
    val idOrThrow
        get() = result.idOrThrow
}
