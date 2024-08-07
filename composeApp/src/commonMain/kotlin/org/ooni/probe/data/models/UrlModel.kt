package org.ooni.probe.data.models

data class UrlModel(
    val id: Id? = null,
    val url: String,
    val categoryCode: String?,
    val countryCode: String?,
) {
    data class Id(
        val value: Long,
    )
}
