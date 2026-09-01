package org.ooni.engine

import android.net.ConnectivityManager
import android.os.Build
import co.touchlab.kermit.Logger
import org.ooni.engine.models.NetworkType
import org.ooni.engine.models.ResolverType

/**
 * Determines the active network's [ResolverType] on Android using [networkTypeFinder]
 * for network transport detection and Android LinkProperties for Private DNS detection.
 */
class AndroidResolverTypeFinder(
    private val connectivityManager: ConnectivityManager?,
    private val networkTypeFinder: NetworkTypeFinder = AndroidNetworkTypeFinder(connectivityManager),
) : ResolverTypeFinder {
    override fun invoke(): ResolverType {
        val networkType = networkTypeFinder()
        val manager = connectivityManager ?: return resolverType(networkType, null)
        return try {
            val isPrivateDnsActive =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val network = manager.activeNetwork
                    val linkProperties = network?.let { manager.getLinkProperties(it) }
                    linkProperties?.isPrivateDnsActive
                } else {
                    null
                }
            resolverType(networkType, isPrivateDnsActive)
        } catch (e: Throwable) {
            Logger.w("Error reading resolver type: ${e.message}")
            resolverType(networkType, null)
        }
    }

    private fun resolverType(
        networkType: NetworkType,
        isPrivateDnsActive: Boolean?,
    ): ResolverType =
        when {
            isPrivateDnsActive == true -> ResolverType.PrivateDns
            networkType is NetworkType.VPN -> ResolverType.VPN
            isPrivateDnsActive == false && networkType !is NetworkType.NoInternet -> ResolverType.System
            else -> ResolverType.Unknown
        }
}
