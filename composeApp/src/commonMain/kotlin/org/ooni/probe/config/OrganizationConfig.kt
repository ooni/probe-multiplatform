package org.ooni.probe.config

import org.jetbrains.compose.resources.DrawableResource

interface OrganizationConfigInterface {
    val baseSoftwareName: String
    val testDisplayMode: TestDisplayMode
    val autorunTaskId: String
    val onboardingImages: OnboardingImages
    val updateDescriptorTaskId: String
    val hasWebsitesDescriptor: Boolean

    val ooniApiBaseUrl get() = BuildTypesDefaults.ooniApiBaseUrl
    val ooniRunDomain get() = BuildTypesDefaults.ooniRunDomain
    val ooniRunDashboardUrl get() = BuildTypesDefaults.ooniRunDashboardUrl
    val explorerUrl get() = BuildTypesDefaults.explorerUrl
}

interface BuildTypesDefaultsInterface {
    val ooniApiBaseUrl: String
    val ooniRunDomain: String
    val ooniRunDashboardUrl: String
    val explorerUrl: String
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
