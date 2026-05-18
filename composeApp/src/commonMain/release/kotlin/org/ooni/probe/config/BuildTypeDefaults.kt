package org.ooni.probe.config

object BuildTypeDefaults : BuildTypeDefaultsInterface {
    override val ooniApiBaseUrl = "https://api.ooni.org"
    override val ooniRunDomain = "run.ooni.org"
    override val ooniRunDashboardUrl = "https://run.ooni.org"
    override val explorerUrl = "https://explorer.ooni.org"
}
