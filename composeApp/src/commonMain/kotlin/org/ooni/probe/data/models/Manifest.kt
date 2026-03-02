package org.ooni.probe.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Manifest(
    @SerialName("manifest") val manifest: Inner,
    @SerialName("meta") val meta: Meta,
) {
    @Serializable
    data class Inner(
        @SerialName("nym_scope") val nymScope: String,
        @SerialName("submission_policy") val submissionPolicy: JsonObject? = null,
        @SerialName("public_parameters") val publicParameters: String,
    )

    @Serializable
    data class Meta(
        @SerialName("version") val version: String,
        @SerialName("last_modification_date") val lastModificationDate: String,
        @SerialName("manifest_url") val manifestUrl: String,
    )
}
