package org.ooni.shared

import co.touchlab.kermit.Logger
import org.ooni.probe.platform
import org.ooni.probe.shared.DesktopOS
import java.io.File

// Windows DLL directory setup flag
private var windowsDllDirectorySet = false

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
        // On Windows, we need to ensure the DLL search path includes the resources directory
        // This must be done before loading any DLL that depends on libwinpthread-1.dll
        if (platform.os == DesktopOS.Windows && !windowsDllDirectorySet) {
            setWindowsDllSearchPath(resourcesPath)
            windowsDllDirectorySet = true
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

// Set Windows DLL search path by modifying PATH environment variable
private fun setWindowsDllSearchPath(resourcesPath: String) {
    try {
        // On Windows, we modify the PATH environment variable to include the resources directory
        // This ensures that DLL dependencies like libwinpthread-1.dll can be found
        val currentPath = System.getenv("PATH") ?: ""

        // Use reflection to modify the environment at runtime
        val envClass = Class.forName("java.lang.ProcessEnvironment")
        val envField = envClass.getDeclaredField("theEnvironment")
        envField.isAccessible = true

        val env = envField.get(null) as MutableMap<String, String>
        val newPath = "$resourcesPath;$currentPath"
        env["PATH"] = newPath

        // Also update the unmodifiable view
        val envCaseInsensitiveField = envClass.getDeclaredField("theCaseInsensitiveEnvironment")
        envCaseInsensitiveField.isAccessible = true

        val envCaseInsensitive = envCaseInsensitiveField.get(null) as MutableMap<String, String>
        envCaseInsensitive["PATH"] = newPath

        Logger.d("Updated Windows PATH to include DLL directory: $resourcesPath")
    } catch (e: Exception) {
        Logger.w("Could not modify PATH environment variable: ${e.message}")
        Logger.w("DLL dependencies may not be found. Ensure libwinpthread-1.dll is in the same directory as other DLLs.")
    }
}

private fun getLibraryFileForOs(name: String) =
    when (platform.os) {
        DesktopOS.Windows -> "$name.dll"
        DesktopOS.Mac -> "lib$name.dylib"
        else -> "lib$name.so"
    }
