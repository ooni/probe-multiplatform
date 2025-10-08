package org.ooni.shared

import co.touchlab.kermit.Logger
import org.ooni.probe.platform
import org.ooni.probe.shared.DesktopOS
import java.io.File

fun loadNativeLibrary(libraryName: String): Boolean {
    val resourcesPath = System.getProperty("compose.application.resources.dir")
    if (resourcesPath != null) {
        // Load from resources directory
        val fileName = getLibraryFileForOs(libraryName)
        val libraryPath = resourcesPath + File.separator + fileName
        try {
            @Suppress("UnsafeDynamicallyLoadedCode")
            System.load(libraryPath)
            Logger.d("Successfully loaded $libraryName library from resources: $libraryPath")
            return true
        } catch (e: UnsatisfiedLinkError) {
            Logger.w(
                "Failed to load $libraryName library from resources ($libraryPath), trying system library path.",
                e,
            )
        }
    } else {
        // Fallback to system library path
        Logger.d("compose.application.resources.dir not set, using system library path")
    }

    try {
        System.loadLibrary(libraryName)
        return true
    } catch (e: UnsatisfiedLinkError) {
        Logger.w("Failed to load native library $libraryName", e)
        return false
    }
}

private fun getLibraryFileForOs(name: String) =
    when (platform.os) {
        DesktopOS.Windows -> "$name.dll"
        DesktopOS.Mac -> "lib$name.dylib"
        else -> "lib$name.so"
    }
