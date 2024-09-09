package org.ooni.probe.config

interface OrganizationConfigInterface {
    val baseSoftwareName: String

    val ooniApiBaseUrl: String
    val ooniRunDashboardUrl: String

    val testDisplayMode: TestDisplayMode
}

enum class TestDisplayMode {
    Regular,
    WebsitesOnly,
}
