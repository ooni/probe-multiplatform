package org.ooni.engine

import org.ooni.engine.models.NetworkType
import org.ooni.engine.models.ResolverType

/**
 * Maps observed DNS transports to a [ResolverType].
 *
 * Holds one instance per [ResolverTypeFinder] so the last fresh verdict is scoped to that finder.
 */
class ResolverTypeDetector {
    /** Last fresh (non-cache) Private DNS verdict, reused for cache hits. */
    private var lastIsPrivateDnsActive: Boolean? = null

    /**
     * Returns the [ResolverType] for the current network and observed DNS protocols.
     *
     * @param dnsProtocols protocol/source names (e.g. `["https"]`, `["udp"]`, `["cache"]`),
     * or `null` on failure or timeout.
     */
    fun resolverType(
        networkType: NetworkType,
        dnsProtocols: List<String>?,
    ): ResolverType {
        if (networkType is NetworkType.NoInternet) return ResolverType.Unknown
        return ResolverType.from(
            isPrivateDnsActive = isPrivateDnsActive(dnsProtocols),
            networkType = networkType,
        )
    }

    /** Private DNS verdict from observed transports; cache hits reuse the last fresh verdict. */
    private fun isPrivateDnsActive(dnsProtocols: List<String>?): Boolean? {
        val protocols = dnsProtocols
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        return when {
            protocols.any { it == PROTOCOL_HTTPS || it == PROTOCOL_TLS } ->
                true.also { lastIsPrivateDnsActive = true }

            protocols.any { it == PROTOCOL_UDP || it == PROTOCOL_TCP } ->
                false.also { lastIsPrivateDnsActive = false }

            protocols.any { it == SOURCE_CACHE } ->
                lastIsPrivateDnsActive ?: false

            else -> null
        }
    }

    private companion object {
        const val PROTOCOL_HTTPS = "https"
        const val PROTOCOL_TLS = "tls"
        const val PROTOCOL_UDP = "udp"
        const val PROTOCOL_TCP = "tcp"
        const val SOURCE_CACHE = "cache"
    }
}
