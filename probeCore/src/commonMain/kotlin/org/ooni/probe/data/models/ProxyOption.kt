package org.ooni.probe.data.models

sealed class ProxyOption(
    val value: String,
) {
    data object None : ProxyOption("")

    data object Psiphon : ProxyOption("psiphon://")

    data class Custom(
        val customValue: String,
    ) : ProxyOption(customValue) {
        companion object {
            fun build(
                protocol: String,
                hostname: String,
                port: String,
            ): Custom {
                val customProtocol = CustomProxyProtocol.entries
                    .firstOrNull { it.value.equals(protocol, ignoreCase = true) }
                    ?: CustomProxyProtocol.HTTP

                val formattedHost = if (hostname.matches(IPV6_ADDRESS_REGEX.toRegex())) {
                    "[$hostname]"
                } else {
                    hostname
                }
                return Custom("${customProtocol.value}://$formattedHost:$port/")
            }
        }
    }
}

enum class CustomProxyProtocol(
    val value: String,
) {
    HTTP("http"),
    HTTPS("https"),
    SOCKS5("socks5"),
}

fun validatePort(port: String): Boolean {
    val portInt = port.toIntOrNull() ?: return false
    return portInt in 1..65535
}

fun validateHost(host: String): Boolean =
    host.matches(IP_ADDRESS_REGEX.toRegex()) ||
        host.matches(IPV6_ADDRESS_REGEX.toRegex()) ||
        host.matches(DOMAIN_NAME_REGEX.toRegex())

private const val IP_ADDRESS_REGEX =
    "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]" +
        "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]" +
        "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}" +
        "|[1-9][0-9]|[0-9]))"

/**
 * Regex for an IPv6 Address.
 *
 * Note that this value is adapted from the following StackOverflow answer:
 * https://stackoverflow.com/a/17871737/1478764
 */
private const val IPV6_ADDRESS_REGEX =
    "(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}" +
        "|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|" +
        "([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|" +
        "[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4})" +
        "{0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.)" +
        "{3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}" +
        "[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))"

private const val DOMAIN_NAME_REGEX = "((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}"
