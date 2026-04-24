package org.ooni.testing.factories

import org.ooni.probe.data.models.Manifest

object ManifestFactory {
    fun build(submissionPolicy: List<Manifest.SubmissionPolicyEntry> = listOf(defaultPolicy())) =
        Manifest(
            manifest = Manifest.Inner(
                nymScope = "SCOPE",
                submissionPolicy = submissionPolicy,
                publicParameters = "PARAMS",
            ),
            meta = Manifest.Meta(
                version = "VERSION",
                lastModificationDate = "2026-03-02T18:28:59.841Z",
                manifestUrl = "https://example.org",
            ),
        )

    fun defaultPolicy() =
        Manifest.SubmissionPolicyEntry(
            match = Manifest.SubmissionPolicyEntry.Match(probeCc = "*", probeAsn = "*"),
            policy = Manifest.SubmissionPolicyEntry.Policy(
                age = listOf(0u, 1_000_000u),
                measurementCount = listOf(0u, 10_000_000u),
            ),
        )
}
