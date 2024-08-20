package org.ooni.engine.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OONINetTest(
    @SerialName("test_name") val name: String,
    @SerialName("inputs") val inputs: List<String>? = null,
)
