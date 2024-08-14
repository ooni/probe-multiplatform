package org.ooni.probe.data.models

import org.ooni.engine.models.WebConnectivityCategory

data class UrlModel(
    val id: Id? = null,
    val url: String,
    val category: WebConnectivityCategory?,
    val countryCode: String?,
) {
    data class Id(
        val value: Long,
    )
}
