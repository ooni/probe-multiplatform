package org.ooni.engine

import org.ooni.engine.models.NetworkType
import org.ooni.engine.models.ResolverType
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultResolverTypeFinderTest {
    @Test
    fun returnsVpnWhenNetworkTypeIsVpn() {
        val finder = DefaultResolverTypeFinder { NetworkType.VPN }
        assertEquals(ResolverType.VPN, finder())
    }

    @Test
    fun returnsSystemWhenNetworkTypeIsWifiMobileOrEthernet() {
        val finderWifi = DefaultResolverTypeFinder { NetworkType.Wifi }
        assertEquals(ResolverType.System, finderWifi())

        val finderMobile = DefaultResolverTypeFinder { NetworkType.Mobile }
        assertEquals(ResolverType.System, finderMobile())

        val finderEthernet = DefaultResolverTypeFinder { NetworkType.Ethernet }
        assertEquals(ResolverType.System, finderEthernet())
    }

    @Test
    fun returnsUnknownWhenNoInternet() {
        val finderNoInternet = DefaultResolverTypeFinder { NetworkType.NoInternet }
        assertEquals(ResolverType.Unknown, finderNoInternet())
    }
}
