package org.ooni.probe.data.models

data class ResultListItem(
    val result: ResultModel,
    val network: NetworkModel?,
    val measurementsCount: Long,
) {
    val idOrThrow get() = result.idOrThrow
}
