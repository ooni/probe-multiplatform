package org.ooni.probe.config

import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.onboarding

object OrganizationConfig : OrganizationConfigInterface {
    override val baseSoftwareName = "news-media-scan"
    override val appId = "com.dw.ooniprobe"
    override val testDisplayMode = TestDisplayMode.WebsitesOnly
    override val autorunTaskId = "org.dw.probe.autorun-task"
    override val updateDescriptorTaskId = "org.dw.probe.update-descriptor-task"
    override val onboardingImages = OnboardingImages(
        image1 = Res.drawable.onboarding,
        image2 = Res.drawable.onboarding,
        image3 = Res.drawable.onboarding,
    )
    override val hasWebsitesDescriptor = false
    override val donateUrl = null
    override val installUrl = null
    override val hasOoniNews = false
    override val canInstallDescriptors = false
}
