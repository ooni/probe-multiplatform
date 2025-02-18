package org.ooni.probe.data.models

import co.touchlab.kermit.Logger
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom
import ooniprobe.composeapp.generated.resources.Settings_Proxy_None
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Psiphon
import org.jetbrains.compose.resources.StringResource

const val IP_ADDRESS = (
    "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]" +
        "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]" +
        "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}" +
        "|[1-9][0-9]|[0-9]))"
)

/**
 * Regex for an IPv6 Address.
 *
 * Note that this value is adapted from the following StackOverflow answer:
 * https://stackoverflow.com/a/17871737/1478764
 */
const val IPV6_ADDRESS =
    (
        "(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}" +
            "|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|" +
            "([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|" +
            "[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4})" +
            "{0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.)" +
            "{3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}" +
            "[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))"
    )

const val DOMAIN_NAME = ("((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}")

enum class ProxyType(val label: StringResource) {
    NONE(label = Res.string.Settings_Proxy_None),
    PSIPHON(label = Res.string.Settings_Proxy_Psiphon),
    CUSTOM(label = Res.string.Settings_Proxy_Custom),
}

enum class ProxyProtocol(val value: String) {
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
            else -> this.value
        }
    }

    companion object {
        fun fromValue(value: String?) = value?.let { entries.firstOrNull { it.value == value } } ?: NONE
    }
}

/**
 * ProxySettings contains the settings configured inside the proxy. Please, see the
 * documentation of proxy activity for the design rationale.
 */
sealed interface ProxySettings {
    fun getProxyString(): String

    data object None : ProxySettings {
        override fun getProxyString() = ""
    }

    data object Psiphon : ProxySettings {
        override fun getProxyString() = "psiphon://"
    }

    data class Custom(
        val protocol: ProxyProtocol,
        val hostname: String,
        val port: Int,
    ) : ProxySettings {
        override fun getProxyString(): String {
            val formattedHost = if (isIPv6(hostname)) {
                "[$hostname]"
            } else {
                hostname
            }
            return "${protocol.value}://$formattedHost:$port/"
        }

        private fun isIPv6(hostname: String): Boolean {
            return IPV6_ADDRESS.toRegex().matches(hostname)
        }
    }

    companion object {
        /**
         * Creates a new ProxySettings object from the given protocol, hostname, and port.
         *
         * @param protocol the proxy protocol
         * @param hostname the proxy hostname
         * @param port the proxy port
         */
        fun newProxySettings(
            protocol: String?,
            hostname: String?,
            port: Int?,
        ): ProxySettings =
            when (protocol) {
                ProxyProtocol.NONE.value -> None
                ProxyProtocol.PSIPHON.value -> Psiphon

                ProxyProtocol.SOCKS5.value,
                ProxyProtocol.HTTP.value,
                ProxyProtocol.HTTPS.value,
                -> run {
                    Custom(
                        protocol = ProxyProtocol.fromValue(protocol),
                        hostname = hostname ?: return@run null,
                        port = port ?: return@run null,
                    )
                }

                else -> null
            } ?: run {
                Logger.w(
                    "Invalid proxy settings: protocol=$protocol hostname=$hostname port=$port",
                    InvalidSettings(),
                )
                None
            }
    }

    class InvalidSettings : Exception("Invalid proxy settings")
}
