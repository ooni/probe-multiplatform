package org.ooni.probe.data.models

import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom
import ooniprobe.composeapp.generated.resources.Settings_Proxy_None
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Psiphon
import org.jetbrains.compose.resources.StringResource

enum class ProxyType(val label: StringResource, val value: String) {
    NONE(
        label = Res.string.Settings_Proxy_None,
        value = "none",
    ),
    PSIPHON(
        label = Res.string.Settings_Proxy_Psiphon,
        value = "psiphon",
    ),
    CUSTOM(label = Res.string.Settings_Proxy_Custom, value = "custom"),
}

enum class ProxyProtocol(val protocol: String) {
    NONE("none"),
    HTTP("http"),
    HTTPS("https"),
    SOCKS5("socks5"),
    PSIPHON("psiphon"),
    ;

    fun proxyType(): ProxyType {
        return when (this) {
            NONE -> ProxyType.NONE
            PSIPHON -> ProxyType.PSIPHON
            else -> ProxyType.CUSTOM
        }
    }

    fun toCustomProtocol(): String {
        return when (this) {
            PSIPHON, NONE -> ""
            else -> this.protocol
        }
    }
}
