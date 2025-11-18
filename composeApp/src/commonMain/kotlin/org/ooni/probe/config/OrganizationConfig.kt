package org.ooni.probe.config

import org.jetbrains.compose.resources.DrawableResource

interface OrganizationConfigInterface {
    val baseSoftwareName: String
    val testDisplayMode: TestDisplayMode
    val autorunTaskId: String
    val onboardingImages: OnboardingImages
    val updateDescriptorTaskId: String
    val hasWebsitesDescriptor: Boolean
    val donateUrl: String?
    val hasOoniNews: Boolean
    val canInstallDescriptors: Boolean

    val ooniApiBaseUrl get() = BuildTypeDefaults.ooniApiBaseUrl
    val ooniRunDomain get() = BuildTypeDefaults.ooniRunDomain
    val ooniRunDashboardUrl get() = BuildTypeDefaults.ooniRunDashboardUrl
    val explorerUrl get() = BuildTypeDefaults.explorerUrl
}

interface BuildTypeDefaultsInterface {
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
