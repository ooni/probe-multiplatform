package org.ooni.probe.config

object OrganizationConfig : OrganizationConfigInterface {
    override val baseSoftwareName = "ooniprobe"
    override val ooniApiBaseUrl = "https://api.dev.ooni.io"
    override val testDisplayMode = TestDisplayMode.Regular
    override val autorunTaskId = "org.ooni.probe.autorun-task"
    override val updateDescriptorTaskId = "org.ooni.probe.update-descriptor-task-task"
}
