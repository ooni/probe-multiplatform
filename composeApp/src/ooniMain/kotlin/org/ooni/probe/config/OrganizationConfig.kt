package org.ooni.probe.config

import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.onboarding1
import ooniprobe.composeapp.generated.resources.onboarding2
import ooniprobe.composeapp.generated.resources.onboarding3

object OrganizationConfig : OrganizationConfigInterface {
    override val baseSoftwareName = "ooniprobe"
    override val appId = "org.openobservatory.ooniprobe"
    override val testDisplayMode = TestDisplayMode.Regular
    override val autorunTaskId = "org.ooni.probe.autorun-task"
    override val updateDescriptorTaskId = "org.ooni.probe.update-descriptor-task"
    override val onboardingImages = OnboardingImages(
        image1 = Res.drawable.onboarding1,
        image2 = Res.drawable.onboarding2,
        image3 = Res.drawable.onboarding3,
    )
    override val hasWebsitesDescriptor = true
    override val donateUrl = "https://ooni.org/donate"
    override val installUrl: String? = "https://ooni.org/install/mobile"
    override val hasOoniNews = true
    override val canInstallDescriptors = true
    override val descriptorLanguageCodes =
        setOf(
            "ar",
            "ca",
            "de",
            "el",
            "es",
            "fa",
            "fr",
            "hi",
            "id",
            "is",
            "it",
            "nl",
            "pt_BR",
            "ro",
            "ru",
            "sw",
            "sk",
            "sq",
            "th",
            "tr",
            "zh_CN",
            "zh_TW",
            "my",
            "vi",
            "km",
        )
}
