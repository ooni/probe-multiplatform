package org.ooni.probe.config

import org.jetbrains.compose.resources.DrawableResource

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

data class OnboardingImages(
    val image1: DrawableResource,
    val image2: DrawableResource,
    val image3: DrawableResource,
)

enum class TestDisplayMode {
    Regular,
    WebsitesOnly,
}
