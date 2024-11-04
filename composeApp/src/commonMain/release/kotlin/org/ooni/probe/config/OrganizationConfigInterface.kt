package org.ooni.probe.config

interface OrganizationConfigInterface {
    val baseSoftwareName: String

    val ooniApiBaseUrl: String
        get() = "https://api.ooni.org"

    val ooniRunDomain: String
        get() = "run.ooni.org"

    val ooniRunDashboardUrl: String
        get() = "https://run.ooni.org"

    val testDisplayMode: TestDisplayMode

    val autorunTaskId: String

    val onboardingImages: OnboardingImages

    val updateDescriptorTaskId: String
}
