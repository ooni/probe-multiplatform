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
                val resourcesPath = System.getProperty("compose.application.resources.dir")
                if (resourcesPath != null) {
                    // Load from resources directory
                    val libraryPath = when {
                        System.getProperty("os.name").contains("Windows", ignoreCase = true) -> 
                            "$resourcesPath\\networktypefinder.dll"
                        System.getProperty("os.name").contains("Mac", ignoreCase = true) -> 
                            "$resourcesPath/libnetworktypefinder.dylib"
                        else -> 
                            "$resourcesPath/libnetworktypefinder.so"
                    }
                    try {
                        System.load(libraryPath)
                        Logger.d("Successfully loaded networktypefinder library from resources: $libraryPath")
                        libraryLoaded = true
                    } catch (e: UnsatisfiedLinkError) {
                        Logger.w("Failed to load networktypefinder library from resources ($libraryPath), trying system library path:", e)
                        System.loadLibrary("networktypefinder")
                        libraryLoaded = true
                    }
                } else {
                    // Fallback to system library path
                    Logger.d("compose.application.resources.dir not set, using system library path")
                    System.loadLibrary("networktypefinder")
                    libraryLoaded = true
                }
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
            "wired_ethernet" -> NetworkType.Ethernet
            "no_internet" -> NetworkType.NoInternet
            else -> NetworkType.Unknown(networkTypeString)
        }
    }
}
