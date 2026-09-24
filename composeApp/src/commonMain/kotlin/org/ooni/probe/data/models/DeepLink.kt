package org.ooni.probe.data.models

sealed class DeepLink {
    data class AddDescriptor(
        val id: String,
    ) : DeepLink()

    data class RunUrls(
        val url: String,
    ) : DeepLink()

    /** `ooni://login?token=<token>`, from the login link emailed by the OONI Run dashboard. */
    data class Login(
        val token: String,
    ) : DeepLink()

    data object Error : DeepLink()
}
