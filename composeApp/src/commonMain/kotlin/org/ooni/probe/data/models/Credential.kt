package org.ooni.probe.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CredentialBody(
    @SerialName("credential_sign_response")
    val credentialSignedResponse: String,
    @SerialName("emission_day")
    val emissionDay: UInt,
)

@Serializable
data class Credential(
    @SerialName("credential")
    val credential: String,
    @SerialName("emission_day")
    val emissionDay: UInt,
)
