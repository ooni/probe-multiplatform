package org.ooni.probe.config

object BuildTypeDefaults : BuildTypeDefaultsInterface {
    override val ooniApiBaseUrl = "https://api.dev.ooni.io"
    override val ooniRunDomain = "run.test.ooni.org"
    override val ooniRunDashboardUrl = "https://run.ooni.org"
    override val explorerUrl = "https://explorer.ooni.org"
}
