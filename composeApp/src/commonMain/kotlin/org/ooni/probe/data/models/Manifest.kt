package org.ooni.probe.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Manifest(
    @SerialName("manifest") val manifest: Inner,
    @SerialName("meta") val meta: Meta,
) {
    @Serializable
    data class Inner(
        @SerialName("nym_scope") val nymScope: String,
        @SerialName("submission_policy") val submissionPolicy: List<SubmissionPolicyEntry> = emptyList(),
        @SerialName("public_parameters") val publicParameters: String,
    )

    @Serializable
    data class Meta(
        @SerialName("version") val version: String,
        @SerialName("last_modification_date") val lastModificationDate: String,
        @SerialName("manifest_url") val manifestUrl: String,
        @SerialName("library_version") val libraryVersion: String? = null,
        @SerialName("protocol_version") val protocolVersion: String? = null,
    )

    @Serializable
    data class SubmissionPolicyEntry(
        @SerialName("match") val match: Match,
        @SerialName("policy") val policy: Policy,
    ) {
        @Serializable
        data class Match(
            @SerialName("probe_cc") val probeCc: String,
            @SerialName("probe_asn") val probeAsn: String,
        )

        @Serializable
        data class Policy(
            @SerialName("age") val age: List<UInt>,
            @SerialName("measurement_count") val measurementCount: List<UInt>,
        )
    }
}
