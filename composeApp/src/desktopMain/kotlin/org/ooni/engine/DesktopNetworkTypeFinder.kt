package org.ooni.engine

import co.touchlab.kermit.Logger
import org.ooni.engine.models.NetworkType
import org.ooni.shared.DesktopBridgeLoader

/** * DesktopNetworkTypeFinder is a class that implements NetworkTypeFinder
 * to determine the network type on desktop platforms.
 */
class DesktopNetworkTypeFinder : NetworkTypeFinder {
    private external fun getNetworkType(): String

    override fun invoke(): NetworkType {
        val networkTypeString = try {
            if (DesktopBridgeLoader.ensureLoaded()) {
                getNetworkType()
            } else {
                "unknown"
            }
        } catch (e: Throwable) {
            Logger.w("Error in native method call: ${e.message}")
            "unknown"
        }

        return when (networkTypeString) {
            "vpn" -> NetworkType.VPN
            "wifi" -> NetworkType.Wifi
            "mobile" -> NetworkType.Mobile
            "wired_ethernet" -> NetworkType.Ethernet
            "no_internet" -> NetworkType.NoInternet
            else -> NetworkType.Unknown(networkTypeString)
        }
    }
}
