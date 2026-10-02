package org.ooni.probe.data.models

import kotlin.test.Test
import kotlin.test.assertEquals

class ProxyOptionTest {
    @Test
    fun buildCustomHttp() {
        val option = ProxyOption.Custom.build(
            "http",
            "example.org",
            "80",
        )
        assertEquals("http://example.org:80/", option.value)
    }

    @Test
    fun buildCustomHttpIPv6() {
        val option = ProxyOption.Custom.build(
            "https",
            "2001:db8:85a3:8d3:1319:8a2e:370:7348",
            "1234",
        )
        assertEquals("https://[2001:db8:85a3:8d3:1319:8a2e:370:7348]:1234/", option.value)
    }

    @Test
    fun buildInvalidProtocol() {
        val option = ProxyOption.Custom.build(
            "ooni",
            "example.org",
            "80",
        )
        assertEquals("http://example.org:80/", option.value)
    }

    @Test
    fun buildWithUsernameAndPassword() {
        val option = ProxyOption.Custom.build(
            protocol = "socks5",
            hostname = "example.org",
            port = "80",
            username = "username",
            password = "password",
        )
        assertEquals("socks5://username:password@example.org:80/", option.value)
    }

    @Test
    fun displayValueMasksPasswordWhenPresent() {
        val option = ProxyOption.Custom.build(
            protocol = "socks5",
            hostname = "example.org",
            port = "80",
            username = "username",
            password = "password",
        )
        assertEquals("socks5://username:***@example.org:80/", option.displayValue)

        val optionIpv6 = ProxyOption.Custom.build(
            protocol = "https",
            hostname = "2001:db8:85a3:8d3:1319:8a2e:370:7348",
            port = "1234",
            username = "username",
            password = "password",
        )
        assertEquals("https://username:***@[2001:db8:85a3:8d3:1319:8a2e:370:7348]:1234/", optionIpv6.displayValue)
    }

    @Test
    fun displayValueLeavesUrlUnchangedWhenNoPasswordPresent() {
        val optionWithoutAuth = ProxyOption.Custom.build(
            protocol = "socks5",
            hostname = "example.org",
            port = "80",
        )
        assertEquals("socks5://example.org:80/", optionWithoutAuth.displayValue)

        val optionWithUsernameOnly = ProxyOption.Custom.build(
            protocol = "socks5",
            hostname = "example.org",
            port = "80",
            username = "username",
        )
        assertEquals("socks5://username@example.org:80/", optionWithUsernameOnly.displayValue)
    }

    @Test
    fun validateUsernameRequiresUsernameWhenPasswordIsPresent() {
        kotlin.test.assertFalse(validateUsername(username = "", password = "password"))
        kotlin.test.assertFalse(validateUsername(username = "   ", password = "password"))
        kotlin.test.assertTrue(validateUsername(username = "username", password = "password"))
        kotlin.test.assertTrue(validateUsername(username = "username", password = ""))
        kotlin.test.assertTrue(validateUsername(username = "", password = ""))
    }
}
