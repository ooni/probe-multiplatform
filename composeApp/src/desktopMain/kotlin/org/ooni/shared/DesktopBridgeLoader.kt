package org.ooni.shared

import co.touchlab.kermit.Logger

object DesktopBridgeLoader {
    private val isLoaded: Boolean by lazy {
        val loaded = loadNativeLibrary("desktopbridge")
        if (loaded) {
            Logger.d("DesktopBridgeLoader: Loaded desktopbridge native library")
        } else {
            Logger.w("DesktopBridgeLoader: Failed to load desktopbridge native library")
        }
        loaded
    }

    fun ensureLoaded(): Boolean = isLoaded
}
