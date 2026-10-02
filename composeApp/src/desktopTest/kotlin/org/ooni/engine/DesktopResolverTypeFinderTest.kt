package org.ooni.engine

import org.ooni.engine.models.NetworkType
import org.ooni.engine.models.ResolverType
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopResolverTypeFinderTest {
    @Test
    fun encryptedDnsIsPrivateDns() {
        assertEquals(ResolverType.PrivateDns, buildFinder(protocols = listOf("https"))())
        assertEquals(ResolverType.PrivateDns, buildFinder(protocols = listOf("tls"))())
        assertEquals(ResolverType.PrivateDns, buildFinder(protocols = listOf("udp", "https"))())
    }

    @Test
    fun plaintextDnsIsSystem() {
        assertEquals(ResolverType.System, buildFinder(protocols = listOf("udp"))())
        assertEquals(ResolverType.System, buildFinder(protocols = listOf("tcp"))())
    }

    @Test
    fun vpnWithPlaintextDnsIsVpn() {
        assertEquals(ResolverType.VPN, buildFinder(networkType = NetworkType.VPN, protocols = listOf("udp"))())
    }

    @Test
    fun noObservationIsUnknown() {
        assertEquals(ResolverType.Unknown, buildFinder(protocols = null)())
        assertEquals(ResolverType.Unknown, buildFinder(protocols = emptyList())())
        assertEquals(ResolverType.Unknown, buildFinder(protocols = listOf("unknown"))())
    }

    @Test
    fun probeFailureIsUnknown() {
        val finder = DesktopResolverTypeFinder(
            networkTypeFinder = { NetworkType.Wifi },
            probeDomain = "probe.example.org",
            probeDnsProtocols = { _, _ -> throw IllegalStateException("boom") },
            mapper = ResolverTypeMapper(),
        )
        assertEquals(ResolverType.Unknown, finder())
    }

    @Test
    fun noInternetOrNoProbeDomainSkipsProbe() {
        var probes = 0
        val probe = { _: String, _: Long ->
            probes++
            listOf("udp")
        }
        val noInternet =
            DesktopResolverTypeFinder(
                { NetworkType.NoInternet },
                "probe.example.org",
                probeDnsProtocols = probe,
                mapper = ResolverTypeMapper(),
            )
        val noDomain = DesktopResolverTypeFinder({ NetworkType.Wifi }, null, probeDnsProtocols = probe, mapper = ResolverTypeMapper())

        assertEquals(ResolverType.Unknown, noInternet())
        assertEquals(ResolverType.Unknown, noDomain())
        assertEquals(0, probes)
    }

    @Test
    fun looksUpProbeDomain() {
        val hosts = mutableListOf<String>()
        val finder = DesktopResolverTypeFinder(
            networkTypeFinder = { NetworkType.Wifi },
            probeDomain = "explorer.dev.ooni.org",
            probeDnsProtocols = { host, _ ->
                hosts += host
                listOf("udp")
            },
            mapper = ResolverTypeMapper(),
        )
        finder()

        assertEquals(listOf("explorer.dev.ooni.org"), hosts)
    }

    @Test
    fun cachedAnswerReusesLastResult() {
        var protocols = listOf("https")
        val finder = DesktopResolverTypeFinder(
            networkTypeFinder = { NetworkType.Wifi },
            probeDomain = "ooni.org",
            probeDnsProtocols = { _, _ -> protocols },
            mapper = ResolverTypeMapper(),
        )
        assertEquals(ResolverType.PrivateDns, finder())

        protocols = listOf("cache")
        assertEquals(ResolverType.PrivateDns, finder())
    }

    @Test
    fun cachedAnswerWithoutPreviousResultIsSystem() {
        assertEquals(ResolverType.System, buildFinder(protocols = listOf("cache"))())
    }

    @Test
    fun delegatesToCustomMapper() {
        val customMapper = ResolverTypeMapper()
        val finder = DesktopResolverTypeFinder(
            networkTypeFinder = { NetworkType.Wifi },
            probeDomain = "ooni.org",
            probeDnsProtocols = { _, _ -> listOf("https") },
            mapper = customMapper,
        )
        assertEquals(ResolverType.PrivateDns, finder())
    }

    private fun buildFinder(
        networkType: NetworkType = NetworkType.Wifi,
        protocols: List<String>?,
        mapper: ResolverTypeMapper = ResolverTypeMapper(),
    ) = DesktopResolverTypeFinder(
        networkTypeFinder = { networkType },
        probeDomain = "probe.example.org",
        probeDnsProtocols = { _, _ -> protocols },
        mapper = mapper,
    )
}
