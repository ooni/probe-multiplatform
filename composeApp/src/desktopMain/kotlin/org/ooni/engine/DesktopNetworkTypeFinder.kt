package org.ooni.engine

import co.touchlab.kermit.Logger
import org.ooni.engine.models.NetworkType

/** * DesktopNetworkTypeFinder is a class that implements NetworkTypeFinder
 * to determine the network type on desktop platforms.
 */
class DesktopNetworkTypeFinder : NetworkTypeFinder {
    companion object Companion {
        private var libraryLoaded = false

        init {
            try {
                System.loadLibrary("networktypefinder")
                libraryLoaded = true
            } catch (e: UnsatisfiedLinkError) {
                Logger.w("Failed to load native library: ${e.message}")
                libraryLoaded = false
            }
        }

        fun isLibraryLoaded(): Boolean = libraryLoaded
    }

    private external fun getNetworkType(): String

    override fun invoke(): NetworkType {
        val networkTypeString = try {
            if (isLibraryLoaded()) {
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
            "no_internet" -> NetworkType.NoInternet
            else -> NetworkType.Unknown(networkTypeString)
        }
    }
}
