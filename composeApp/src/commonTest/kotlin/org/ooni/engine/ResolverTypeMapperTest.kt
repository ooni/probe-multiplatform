package org.ooni.engine

import org.ooni.engine.models.NetworkType
import org.ooni.engine.models.ResolverType
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolverTypeMapperTest {
    @Test
    fun encryptedDnsIsPrivateDns() {
        assertEquals(ResolverType.PrivateDns, ResolverTypeMapper().resolverType(NetworkType.Wifi, listOf("https")))
        assertEquals(ResolverType.PrivateDns, ResolverTypeMapper().resolverType(NetworkType.Wifi, listOf("tls")))
        assertEquals(ResolverType.PrivateDns, ResolverTypeMapper().resolverType(NetworkType.Wifi, listOf("udp", "https")))
    }

    @Test
    fun plaintextDnsIsSystem() {
        assertEquals(ResolverType.System, ResolverTypeMapper().resolverType(NetworkType.Wifi, listOf("udp")))
        assertEquals(ResolverType.System, ResolverTypeMapper().resolverType(NetworkType.Mobile, listOf("tcp")))
    }

    @Test
    fun vpnWithPlaintextDnsIsVpn() {
        assertEquals(ResolverType.VPN, ResolverTypeMapper().resolverType(NetworkType.VPN, listOf("udp")))
    }

    @Test
    fun vpnWithEncryptedDnsIsPrivateDns() {
        assertEquals(ResolverType.PrivateDns, ResolverTypeMapper().resolverType(NetworkType.VPN, listOf("https")))
    }

    @Test
    fun noInternetIsUnknown() {
        assertEquals(ResolverType.Unknown, ResolverTypeMapper().resolverType(NetworkType.NoInternet, null))
        assertEquals(ResolverType.Unknown, ResolverTypeMapper().resolverType(NetworkType.NoInternet, listOf("udp")))
    }

    @Test
    fun noObservationIsUnknown() {
        assertEquals(ResolverType.Unknown, ResolverTypeMapper().resolverType(NetworkType.Wifi, null))
        assertEquals(ResolverType.Unknown, ResolverTypeMapper().resolverType(NetworkType.Wifi, emptyList()))
        assertEquals(ResolverType.Unknown, ResolverTypeMapper().resolverType(NetworkType.Wifi, listOf("unknown")))
    }

    @Test
    fun cachedAnswerWithoutPreviousResultIsSystem() {
        assertEquals(ResolverType.System, ResolverTypeMapper().resolverType(NetworkType.Wifi, listOf("cache")))
    }

    @Test
    fun cachedAnswerReusesLastFreshResult() {
        val detector = ResolverTypeMapper()
        assertEquals(ResolverType.PrivateDns, detector.resolverType(NetworkType.Wifi, listOf("https")))
        assertEquals(ResolverType.PrivateDns, detector.resolverType(NetworkType.Wifi, listOf("cache")))
    }

    @Test
    fun cachedAnswerReusesLastPlaintextResult() {
        val detector = ResolverTypeMapper()
        assertEquals(ResolverType.System, detector.resolverType(NetworkType.Wifi, listOf("udp")))
        assertEquals(ResolverType.System, detector.resolverType(NetworkType.Wifi, listOf("cache")))
    }
}
