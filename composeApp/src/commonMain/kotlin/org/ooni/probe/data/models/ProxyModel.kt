package org.ooni.probe.data.models

import kotlinx.coroutines.flow.firstOrNull
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom
import ooniprobe.composeapp.generated.resources.Settings_Proxy_None
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Psiphon
import org.jetbrains.compose.resources.StringResource
import org.ooni.probe.data.repositories.PreferenceRepository

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

/**
 * ProxySettings contains the settings configured inside the proxy. Please, see the
 * documentation of proxy activity for the design rationale.
 */
class ProxySettings {
    /**
     * Scheme is the proxy scheme (e.g., "psiphon", "socks5").
     */
    var protocol: ProxyProtocol = ProxyProtocol.NONE

    /**
     * Hostname is the hostname for custom proxies.
     */
    var hostname: String = ""

    /**
     * Port is the port for custom proxies.
     */
    var port: String = ""

    companion object {
        /**
         * Creates a new ProxySettings object from the values stored in the preference repository.
         * @param preferenceRepository the [PreferenceRepository].
         */
        suspend fun newProxySettings(preferenceRepository: PreferenceRepository): ProxySettings {
            val protocol =
                preferenceRepository.getValueByKey(SettingsKey.PROXY_PROTOCOL).firstOrNull() as String?
            val settings = ProxySettings()

            when (protocol) {
                ProxyProtocol.NONE.protocol -> {
                    settings.protocol = ProxyProtocol.NONE
                }
                ProxyProtocol.PSIPHON.protocol -> {
                    settings.protocol = ProxyProtocol.PSIPHON
                }
                ProxyProtocol.SOCKS5.protocol, ProxyProtocol.HTTP.protocol, ProxyProtocol.HTTPS.protocol -> {
                    // ProxyProtocol.valueOf will only accept one of the values in ProxyProtocol
                    // as in the enum definition(uppercase).
                    settings.protocol =
                        ProxyProtocol.valueOf(protocol)
                }
                else -> {
                    // This is where we will extend the code to add support for
                    // more proxies, e.g., HTTP proxies.
                    throw InvalidProxyURL("unhandled URL scheme")
                }
            }

            settings.apply {
                hostname =
                    preferenceRepository.getValueByKey(SettingsKey.PROXY_HOSTNAME).firstOrNull() as String? ?: ""
                port = (preferenceRepository.getValueByKey(SettingsKey.PROXY_PORT).firstOrNull() as Int? ?: "").toString()
            }
            return settings
        }
    }

    fun getProxyString(): String {
        when (protocol) {
            ProxyProtocol.NONE -> return ""
            ProxyProtocol.PSIPHON -> return "psiphon://"
            ProxyProtocol.SOCKS5, ProxyProtocol.HTTP, ProxyProtocol.HTTPS -> {
                val formattedHost = if (isIPv6(hostname)) {
                    "[$hostname]"
                } else {
                    hostname
                }
                return "${protocol.protocol}://$formattedHost:$port/"
            }

            else -> return ""
        }
    }

    private fun isIPv6(hostname: String): Boolean {
        return IPV6_ADDRESS.toRegex().matches(hostname)
    }

    class InvalidProxyURL(message: String) : Exception(message)
}
