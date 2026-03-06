package org.ooni.probe.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Credential(
    @SerialName("credential_sign_response")
    val credential: String,
    @SerialName("emission_day")
    val emissionDay: UInt,
)
