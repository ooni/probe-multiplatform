package org.ooni.probe.data.models

import org.ooni.engine.models.NetworkType

data class NetworkModel(
    val id: Id? = null,
    val networkName: String?,
    val ip: String?,
    val asn: String?,
    val countryCode: String?,
    val networkType: NetworkType?,
) {
    data class Id(
        val value: Long,
    )
}
