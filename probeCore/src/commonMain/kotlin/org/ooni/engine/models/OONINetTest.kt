package org.ooni.engine.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OONINetTest(
    @SerialName("test_name") val name: String,
    @SerialName("inputs") val inputs: List<String>? = null,
    @SerialName("is_background_run_enabled_default") val isBackgroundRunEnabled: Boolean = false,
    @SerialName("is_manual_run_enabled_default") val isManualRunEnabled: Boolean = false,
)
