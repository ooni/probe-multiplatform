package org.ooni.probe.config

import org.ooni.probe.data.models.ProxyType

class DesktopProxyConfig : ProxyConfig {
    override fun getSupportedProxyTypes(): List<ProxyType> = listOf(ProxyType.NONE, ProxyType.CUSTOM)
}
