package org.ooni.engine.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskSettings(
    @SerialName("name") val name: String,
    @SerialName("inputs") val inputs: List<String>,
    @SerialName("version") val version: Int = 1,
    @SerialName("log_level") val logLevel: TaskLogLevel,
    @SerialName("disabled_events") val disabledEvents: List<String> = emptyList(),
    @SerialName("proxy") val proxy: String? = null,
    @SerialName("state_dir") val stateDir: String? = null,
    @SerialName("temp_dir") val tempDir: String? = null,
    @SerialName("tunnel_dir") val tunnelDir: String? = null,
    @SerialName("assets_dir") val assetsDir: String? = null,
    @SerialName("options") val options: Options,
    @SerialName("annotations") val annotations: Annotations,
) {
    @Serializable
    data class Options(
        // upload results or not
        @SerialName("no_collector") val noCollector: Boolean,
        // built from the flavors + debug or not + -unattended if autorun
        @SerialName("software_name") val softwareName: String,
        @SerialName("software_version") val softwareVersion: String,
    )

    @Serializable
    data class Annotations(
        @SerialName("network_type") val networkType: NetworkType,
        // OONI or DW
        @SerialName("flavor") val flavor: String,
        // "autorun" or "ooni-run"
        @SerialName("origin") val origin: TaskOrigin,
    )
}
