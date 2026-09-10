package org.ooni.engine

import org.ooni.engine.models.NetworkType
import org.ooni.engine.models.ResolverType

/**
 * Default implementation of [ResolverTypeFinder] that delegates to a [NetworkTypeFinder].
 * - Returns [ResolverType.VPN] when network type is VPN.
 * - Returns [ResolverType.Unknown] when network type is [NetworkType.NoInternet].
 * - Returns [ResolverType.Unknown] for all other network types when the resolver cannot be determined.
 */
class DefaultResolverTypeFinder(
    private val networkTypeFinder: NetworkTypeFinder,
) : ResolverTypeFinder {
    override fun invoke(): ResolverType =
        when (val networkType = networkTypeFinder()) {
            is NetworkType.VPN -> ResolverType.VPN
            is NetworkType.NoInternet -> ResolverType.Unknown
            else -> ResolverType.Unknown
        }
}
