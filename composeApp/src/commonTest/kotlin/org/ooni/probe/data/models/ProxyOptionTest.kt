package org.ooni.probe.data.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        assertFalse(validateUsername(username = "", password = "password"))
        assertFalse(validateUsername(username = "   ", password = "password"))
        assertTrue(validateUsername(username = "username", password = "password"))
        assertTrue(validateUsername(username = "username", password = ""))
        assertTrue(validateUsername(username = "", password = ""))
    }

    @Test
    fun validateHostAcceptsIpAddresses() {
        assertHostsValid(
            "127.0.0.1",
            "1.2.3.4",
            "192.168.1.100",
            "255.255.255.255",
        )
    }

    @Test
    fun validateHostAcceptsIpV6Addresses() {
        assertHostsValid(
            "::1",
            "2001:db8:85a3:8d3:1319:8a2e:370:7348",
            "2001:db8::1",
        )
    }

    @Test
    fun validateHostAcceptsDomainNames() {
        assertHostsValid(
            "example.org",
            "proxy.example.org",
            "my-proxy.example.co.uk",
            "EXAMPLE.ORG",
            "proxy.example.museum",
            "proxy.example.network",
            "proxy.example.solutions",
            "proxy.example.technology",
            "localhost",
        )
    }

    @Test
    fun validateHostRejectsMalformedHosts() {
        assertHostsInvalid(
            "",
            " ",
            "example.org ",
            " example.org",
            "exa mple.org",
            "example..org",
            ".example.org",
            "example.org.",
            "-example.org",
            "example-.org",
            "-localhost",
            "localhost-",
        )
    }

    @Test
    fun validateHostRejectsHostsWithOtherUrlParts() {
        assertHostsInvalid(
            "http://example.org",
            "example.org:8080",
            "example.org/path",
            "user@example.org",
            "[::1]",
        )
    }

    @Test
    fun validateHostRejectsNumericHostsThatAreNotIpAddresses() {
        assertHostsInvalid("256.256.256.256", "1.2.3", "1234", "example.123")
    }

    private fun assertHostsValid(vararg hosts: String) {
        assertEquals(emptyList(), hosts.filterNot(::validateHost), "Should be valid")
    }

    private fun assertHostsInvalid(vararg hosts: String) {
        assertEquals(emptyList(), hosts.filter(::validateHost), "Should be invalid")
    }
}
