package org.ooni.probe.config

interface OrganizationConfigInterface {
    val baseSoftwareName: String

    val ooniApiBaseUrl: String
        get() = "https://api.dev.ooni.io"

    val ooniRunDomain: String
        get() = "run.test.ooni.org"

    val ooniRunDashboardUrl: String
        get() = "https://run.test.ooni.org"

    val testDisplayMode: TestDisplayMode
}

enum class TestDisplayMode {
    Regular,
    WebsitesOnly,
}
