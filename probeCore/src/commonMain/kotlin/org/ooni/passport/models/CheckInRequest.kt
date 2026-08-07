package org.ooni.passport.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckInRequest(
    @SerialName("run_type") val runType: String,
    @SerialName("charging") val charging: Boolean,
    @SerialName("probe_cc") val probeCc: String,
    @SerialName("probe_asn") val probeAsn: String,
    @SerialName("on_wifi") val onWifi: Boolean?,
    @SerialName("software_name") val softwareName: String,
    @SerialName("software_version") val softwareVersion: String,
    @SerialName("web_connectivity") val webConnectivity: WebConnectivity,
) {
    @Serializable
    data class WebConnectivity(
        @SerialName("category_codes") val categoryCodes: List<String>,
    )
}
