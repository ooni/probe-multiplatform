package org.ooni.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskSettings(
    @SerialName("name") val name: String,
    @SerialName("inputs") val inputs: List<String>,
    @SerialName("version") val version: Int = 1,
    @SerialName("log_level") val logLevel: String,
    @SerialName("state_dir") val stateDir: String? = null,
    @SerialName("temp_dir") val tempDir: String? = null,
    @SerialName("tunnel_dir") val tunnelDir: String? = null,
    @SerialName("assets_dir") val assetsDir: String? = null,
    @SerialName("options") val options: Options = Options()
) {
    @Serializable
    data class Options(
        @SerialName("no_collector") val noCollector: Boolean = true,
        @SerialName("software_name") val softwareName: String = "Probe Multiplatform",
        @SerialName("software_version") val softwareVersion: String = "1.0"
    )
}