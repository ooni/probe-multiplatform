package org.ooni.engine.models

import org.ooni.probe.di.Dependencies
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolverTypeTest {
    private val json = Dependencies.buildJson()

    @Test
    fun fromReturnsPrivateDnsWhenPrivateDnsIsActive() {
        val result = ResolverType.from(
            isPrivateDnsActive = true,
            networkType = NetworkType.Wifi,
        )
        assertEquals(ResolverType.PrivateDns, result)
    }

    @Test
    fun fromReturnsPrivateDnsEvenWhenOnVpnIfPrivateDnsIsActive() {
        val result = ResolverType.from(
            isPrivateDnsActive = true,
            networkType = NetworkType.VPN,
        )
        assertEquals(ResolverType.PrivateDns, result)
    }

    @Test
    fun fromReturnsVpnWhenNetworkTypeIsVpnAndPrivateDnsIsFalseOrNull() {
        val resultFalse = ResolverType.from(
            isPrivateDnsActive = false,
            networkType = NetworkType.VPN,
        )
        assertEquals(ResolverType.VPN, resultFalse)

        val resultNull = ResolverType.from(
            isPrivateDnsActive = null,
            networkType = NetworkType.VPN,
        )
        assertEquals(ResolverType.VPN, resultNull)
    }

    @Test
    fun fromReturnsSystemWhenPrivateDnsIsFalseAndNotVpn() {
        val resultWifi = ResolverType.from(
            isPrivateDnsActive = false,
            networkType = NetworkType.Wifi,
        )
        assertEquals(ResolverType.System, resultWifi)

        val resultMobile = ResolverType.from(
            isPrivateDnsActive = false,
            networkType = NetworkType.Mobile,
        )
        assertEquals(ResolverType.System, resultMobile)
    }

    @Test
    fun fromReturnsUnknownForOtherCases() {
        val resultNullWifi = ResolverType.from(
            isPrivateDnsActive = null,
            networkType = NetworkType.Wifi,
        )
        assertEquals(ResolverType.Unknown, resultNullWifi)

        val resultNullMobile = ResolverType.from(
            isPrivateDnsActive = null,
            networkType = NetworkType.Mobile,
        )
        assertEquals(ResolverType.Unknown, resultNullMobile)
    }

    @Test
    fun serializesToTheAnnotationStringValues() {
        val expected = mapOf(
            ResolverType.PrivateDns to "\"private_dns\"",
            ResolverType.VPN to "\"vpn\"",
            ResolverType.System to "\"system\"",
            ResolverType.Unknown to "\"unknown\"",
        )

        expected.forEach { (type, encoded) ->
            assertEquals(encoded, json.encodeToString(ResolverTypeSerializer, type))
        }
    }

    @Test
    fun deserializesKnownValues() {
        listOf(
            ResolverType.PrivateDns,
            ResolverType.VPN,
            ResolverType.System,
            ResolverType.Unknown,
        ).forEach { type ->
            assertEquals(type, ResolverType.fromValue(type.value))
        }
    }

    @Test
    fun deserializesUnrecognizedValuesAsUnknown() {
        assertEquals(ResolverType.Unknown, ResolverType.fromValue("something_else"))
        assertEquals(ResolverType.Unknown, ResolverType.fromValue(""))
    }
}
