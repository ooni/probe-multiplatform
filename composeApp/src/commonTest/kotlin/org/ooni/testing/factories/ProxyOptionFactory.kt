package org.ooni.testing.factories

import org.ooni.probe.data.models.CustomProxyProtocol
import org.ooni.probe.data.models.ProxyOption

object ProxyOptionFactory {
    fun custom(
        protocol: String = CustomProxyProtocol.HTTP.value,
        hostname: String = "example.org",
        port: String = "80",
    ) = ProxyOption.Custom.build(
        protocol = protocol,
        hostname = hostname,
        port = port,
    )
}
