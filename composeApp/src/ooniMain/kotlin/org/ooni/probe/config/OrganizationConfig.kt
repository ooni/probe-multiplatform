package org.ooni.probe.config

import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.onboarding1
import ooniprobe.composeapp.generated.resources.onboarding2
import ooniprobe.composeapp.generated.resources.onboarding3

object OrganizationConfig : OrganizationConfigInterface {
    override val baseSoftwareName = "ooniprobe"
    override val testDisplayMode = TestDisplayMode.Regular
    override val autorunTaskId = "org.ooni.probe.autorun-task"
    override val updateDescriptorTaskId = "org.ooni.probe.update-descriptor-task"
    override val onboardingImages = OnboardingImages(
        image1 = Res.drawable.onboarding1,
        image2 = Res.drawable.onboarding2,
        image3 = Res.drawable.onboarding3,
    )
}
