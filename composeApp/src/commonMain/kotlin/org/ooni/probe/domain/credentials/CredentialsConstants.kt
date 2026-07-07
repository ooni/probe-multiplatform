package org.ooni.probe.domain.credentials

object CredentialsConstants {
    const val STORAGE_KEY = "credential"

    // Timeout (in seconds) applied to passport HTTP calls (credential/manifest APIs).
    const val HTTP_TIMEOUT_SECONDS: Float = 30f
}
