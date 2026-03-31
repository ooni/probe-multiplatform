package org.ooni.testing.factories

import org.ooni.probe.data.models.Manifest

object ManifestFactory {
    fun build() =
        Manifest(
            manifest = Manifest.Inner(
                nymScope = "SCOPE",
                submissionPolicy = null,
                publicParameters = "PARAMS",
            ),
            meta = Manifest.Meta(
                version = "VERSION",
                lastModificationDate = "2026-03-02T18:28:59.841Z",
                manifestUrl = "https://example.org",
            ),
        )
}
