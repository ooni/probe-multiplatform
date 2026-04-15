package org.ooni.probe.domain.proxy

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.ooni.probe.config.ProxyConfig
import org.ooni.probe.data.models.ProxyOption
import org.ooni.probe.data.models.SettingsKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProxyManagerTest {
    @Test
    fun all() =
        runTest {
            val custom = ProxyOption.Custom("http://example.org:80")
            val subject = ProxyManager(
                getPreference = { key ->
                    flowOf(
                        when (key) {
                            SettingsKey.PROXIES_CUSTOM -> setOf(custom.customValue)
                            SettingsKey.PROXY_SELECTED -> custom.customValue
                            else -> null
                        },
                    )
                },
                setPreference = { _, _ -> },
                removePreference = { _ -> },
                proxyConfig = ProxyConfig(isPsiphonSupported = true),
            )

            val options = subject.all().first()
            assertEquals(options.size, 3)

            assertEquals(ProxyOption.None, options[0].item)
            assertFalse(options[0].isSelected)

            assertEquals(ProxyOption.Psiphon, options[1].item)
            assertFalse(options[1].isSelected)

            assertEquals(custom, options[2].item)
            assertTrue(options[2].isSelected)
        }
}
