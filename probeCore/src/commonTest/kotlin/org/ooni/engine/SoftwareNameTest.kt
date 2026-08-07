package org.ooni.engine

import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.shared.Platform
import kotlin.test.Test
import kotlin.test.assertEquals

class SoftwareNameTest {
    @Test
    fun cliEngineNameOverridesPlatformSuffix() {
        // The CLI runs on Platform.Desktop but must report `ooniprobe-cli`, not `ooniprobe-desktop`.
        assertEquals(
            "ooniprobe-cli",
            buildSoftwareName("ooniprobe", Platform.Desktop("Mac OS X"), TaskOrigin.OoniRun, engineName = "cli"),
        )
    }

    @Test
    fun nullEngineNameKeepsPlatformSuffix() {
        // Regression guard: the apps (null override) keep their platform-derived name.
        assertEquals(
            "ooniprobe-desktop",
            buildSoftwareName("ooniprobe", Platform.Desktop("Mac OS X"), TaskOrigin.OoniRun),
        )
        assertEquals(
            "ooniprobe-android",
            buildSoftwareName("ooniprobe", Platform.Android, TaskOrigin.OoniRun),
        )
        assertEquals(
            "ooniprobe-ios",
            buildSoftwareName("ooniprobe", Platform.Ios, TaskOrigin.OoniRun),
        )
    }

    @Test
    fun autoRunAppendsUnattendedAfterEngineName() {
        assertEquals(
            "ooniprobe-cli-unattended",
            buildSoftwareName("ooniprobe", Platform.Desktop("Linux"), TaskOrigin.AutoRun, engineName = "cli"),
        )
    }
}
