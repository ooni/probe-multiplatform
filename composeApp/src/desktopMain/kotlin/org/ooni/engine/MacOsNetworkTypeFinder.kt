package org.ooni.engine

import co.touchlab.kermit.Logger
import org.ooni.engine.models.NetworkType

/**
 * MacOsNetworkTypeFinder is a class that implements the NetworkTypeFinder interface.
 * It uses a native library to determine the network type on macOS.
 */
class MacOsNetworkTypeFinder : NetworkTypeFinder {
    companion object Companion {
        private var libraryLoaded = false

        init {
            try {
                System.loadLibrary("networktypefinder")
                libraryLoaded = true
            } catch (e: UnsatisfiedLinkError) {
                Logger.d("Failed to load native library: ${e.message}")
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
        } catch (e: UnsatisfiedLinkError) {
            Logger.d("Failed to call native method: ${e.message}")
            "unknown"
        } catch (e: Exception) {
            Logger.d("Exception in native method call: ${e.message}")
            "unknown"
        } catch (e: Throwable) {
            Logger.d("Error in native method call: ${e.message}")
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
