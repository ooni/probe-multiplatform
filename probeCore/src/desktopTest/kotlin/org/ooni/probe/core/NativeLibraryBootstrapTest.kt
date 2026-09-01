package org.ooni.probe.core

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class NativeLibraryBootstrapTest {
    @Test
    fun bundledNativeBootstrapConfiguresAvailableMacOsGoJniDirectory() {
        if (!System.getProperty("os.name").lowercase().contains("mac")) return
        val root = Files.createTempDirectory("core-native-resources").toFile()
        val architecture = when (System.getProperty("os.arch").lowercase()) {
            "aarch64", "arm64" -> "darwin-aarch64"
            "x86_64", "amd64" -> "darwin-x86-64"
            else -> "darwin"
        }
        val library = root.resolve("gojni/$architecture/libgojni.dylib")
        library.parentFile.mkdirs()
        library.writeText("")

        val result = configureBundledNativeLibraries(root)

        assertEquals(listOf("gojni"), result.appliedLibraries)
        assertEquals(library.parentFile.absolutePath, System.getProperty("ooni.gojni.boot.library.path"))
        assertEquals(library.name, System.getProperty("ooni.gojni.boot.library.name"))
    }
}
