package org.ooni.probe.config

object BuildTypesDefaults : BuildTypesDefaultsInterface {
    override val ooniApiBaseUrl = "https://api.dev.ooni.io"
    override val ooniRunDomain = "run.test.ooni.org"
    override val ooniRunDashboardUrl = "https://run.test.ooni.org"
    override val explorerUrl = "https://explorer.test.ooni.org"
}
