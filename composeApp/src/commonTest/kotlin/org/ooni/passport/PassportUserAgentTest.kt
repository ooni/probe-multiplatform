package org.ooni.passport

import org.ooni.probe.SharedBuildConfig
import org.ooni.probe.shared.Platform
import kotlin.test.Test
import kotlin.test.assertEquals

class PassportUserAgentTest {
    @Test
    fun buildUserAgentWithoutProtocolVersion() {
        val userAgent = buildUserAgent(
            softwareName = "ooniprobe",
            platform = Platform.Android,
            engineVersion = "3.18.0",
        )
        val expected = "ooniprobe,${SharedBuildConfig.VERSION_NAME},android," +
            "ooniprobe-engine,3.18.0," +
            "ooniprobe-engine_3.18.0," +
            "ooni-passport,${SharedBuildConfig.PASSPORT_VERSION}"
        assertEquals(expected, userAgent)
    }

    @Test
    fun buildUserAgentWithProtocolVersion() {
        val userAgent = buildUserAgent(
            softwareName = "ooniprobe",
            engineName = "ooniprobe-engine",
            platform = Platform.Android,
            engineVersion = "3.18.0",
            protocolVersion = "1.2.3",
        )
        val expected = "ooniprobe,${SharedBuildConfig.VERSION_NAME},android," +
            "ooniprobe-engine,3.18.0," +
            "ooniprobe-engine_3.18.0," +
            "ooni-passport,${SharedBuildConfig.PASSPORT_VERSION}," +
            "protocol-version,1.2.3"
        assertEquals(expected, userAgent)
    }

    @Test
    fun buildUserAgentWithCustomEngineName() {
        val userAgent = buildUserAgent(
            softwareName = "ooniprobe",
            engineName = "custom-engine",
            platform = Platform.Android,
            engineVersion = "3.18.0",
        )
        val expected = "ooniprobe,${SharedBuildConfig.VERSION_NAME},android," +
            "custom-engine,3.18.0," +
            "custom-engine_3.18.0," +
            "ooni-passport,${SharedBuildConfig.PASSPORT_VERSION}"
        assertEquals(expected, userAgent)
    }
}
