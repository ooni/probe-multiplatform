package org.ooni.probe.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.ooni.engine.models.NetworkType

@Serializable
data class NetworkModel(
    @SerialName("id") val id: Id? = null,
    @SerialName("network_name") val name: String?,
    @SerialName("asn") val asn: String?,
    @SerialName("country_code") val countryCode: String?,
    @SerialName("networkType") val networkType: NetworkType?,
) {
    @Serializable
    data class Id(
        val value: Long,
    )

    fun isValid() = asn != "AS0" && !countryCode.equals("ZZ", ignoreCase = true)
}
