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
    override val supportedLanguageCodes =
        setOf(
            "ar",
            "bs",
            "ca",
            "de",
            "el",
            "en",
            "es",
            "fa",
            "fi",
            "fr",
            "hi",
            "id",
            "is",
            "it",
            "km",
            "my",
            "nl",
            "pt",
            "ro",
            "ru",
            "sk",
            "sq",
            "sw",
            "th",
            "tr",
            "vi",
            "zh",
        )
}
