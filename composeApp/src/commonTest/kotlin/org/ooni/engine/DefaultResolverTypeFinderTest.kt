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
    fun returnsUnknownWhenNetworkTypeIsWifiMobileOrEthernet() {
        val finderWifi = DefaultResolverTypeFinder { NetworkType.Wifi }
        assertEquals(ResolverType.Unknown, finderWifi())

        val finderMobile = DefaultResolverTypeFinder { NetworkType.Mobile }
        assertEquals(ResolverType.Unknown, finderMobile())

        val finderEthernet = DefaultResolverTypeFinder { NetworkType.Ethernet }
        assertEquals(ResolverType.Unknown, finderEthernet())
    }

    @Test
    fun returnsUnknownWhenNoInternet() {
        val finderNoInternet = DefaultResolverTypeFinder { NetworkType.NoInternet }
        assertEquals(ResolverType.Unknown, finderNoInternet())
    }
}
