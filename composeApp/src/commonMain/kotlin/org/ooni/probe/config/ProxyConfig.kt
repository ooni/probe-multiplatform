package org.ooni.probe.config

import org.ooni.probe.data.models.ProxyType

interface ProxyConfig {
    fun getSupportedProxyTypes(): List<ProxyType>
}

class DefaultProxyConfig : ProxyConfig {
    override fun getSupportedProxyTypes(): List<ProxyType> = ProxyType.entries
}
