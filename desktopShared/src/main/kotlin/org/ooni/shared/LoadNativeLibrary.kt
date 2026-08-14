package org.ooni.shared

import co.touchlab.kermit.Logger
import org.ooni.probe.shared.DesktopOS
import org.ooni.probe.shared.Platform
import java.io.File
import java.nio.file.Files

// Windows DLL directory setup flag
private var windowsDllDirectorySet = false

private val desktopOs get() = Platform.Desktop(System.getProperty("os.name")).os

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
        if (desktopOs == DesktopOS.Windows && !windowsDllDirectorySet) {
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
        Logger.w("Failed to load native library $libraryName, trying classpath resources", e)
    }

    // Last resort: the library bundled as a classpath resource (e.g. inside the desktopShared
    // jar on the CLI distribution, where no resources dir or library path is set up).
    return loadBundledNativeLibrary(libraryName)
}

// Extracts `<os>/<lib>` from the jar into a temp directory and loads it from there.
private fun loadBundledNativeLibrary(libraryName: String): Boolean {
    val osDir = when (desktopOs) {
        DesktopOS.Mac -> "macos"
        DesktopOS.Windows -> "windows"
        DesktopOS.Linux -> "linux"
        DesktopOS.Other -> return false
    }
    val fileName = getLibraryFileForOs(libraryName)
    val input = object {}.javaClass.getResourceAsStream("/$osDir/$fileName") ?: return false

    val tempDir = Files.createTempDirectory("ooni-native").apply { toFile().deleteOnExit() }
    // Windows DLLs may depend on sibling DLLs (e.g. libwinpthread-1.dll); extract them too and
    // put the temp directory on the DLL search path before loading.
    if (desktopOs == DesktopOS.Windows) {
        object {}.javaClass.getResourceAsStream("/$osDir/libwinpthread-1.dll")?.use { dep ->
            val depFile = tempDir.resolve("libwinpthread-1.dll").apply { toFile().deleteOnExit() }
            Files.copy(dep, depFile)
        }
        setWindowsDllSearchPath(tempDir.toAbsolutePath().toString())
    }
    val output = tempDir.resolve(fileName).apply { toFile().deleteOnExit() }
    input.use { Files.copy(it, output) }

    return try {
        @Suppress("UnsafeDynamicallyLoadedCode")
        System.load(output.toAbsolutePath().toString())
        Logger.d("Successfully loaded $libraryName library from classpath resource: /$osDir/$fileName")
        true
    } catch (e: UnsatisfiedLinkError) {
        Logger.w("Failed to load native library $libraryName from classpath resource", e)
        false
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
    when (desktopOs) {
        DesktopOS.Windows -> "$name.dll"
        DesktopOS.Mac -> "lib$name.dylib"
        else -> "lib$name.so"
    }
