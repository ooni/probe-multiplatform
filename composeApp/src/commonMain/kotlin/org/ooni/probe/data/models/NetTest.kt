package org.ooni.probe.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetTest(
    @SerialName("test_name")
    val name: String,
    val inputs: List<String>?,
)
