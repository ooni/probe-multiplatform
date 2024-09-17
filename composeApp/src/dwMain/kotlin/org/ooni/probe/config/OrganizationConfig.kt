package org.ooni.probe.config

object OrganizationConfig : OrganizationConfigInterface {
    override val baseSoftwareName = "news-media-scan"
    override val ooniApiBaseUrl = "https://api.prod.ooni.io"
    override val ooniRunDashboardUrl = "https://run-v2.ooni.org"
    override val testDisplayMode = TestDisplayMode.WebsitesOnly
    override val autorunTaskId = "org.dw.probe.autorun-task"
}
