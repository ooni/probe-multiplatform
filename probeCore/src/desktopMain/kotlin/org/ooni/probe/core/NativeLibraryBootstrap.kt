package org.ooni.probe.core

import co.touchlab.kermit.Logger
import java.io.File

private data class BundledNativeLibrary(
    val dirName: String,
    val fileName: String,
    val additionalFiles: List<String> = emptyList(),
    val staticProperties: Map<String, String> = emptyMap(),
    val onResolved: (dir: File, file: File) -> Unit,
)

private val bundledNativeLibraries = listOf(
    BundledNativeLibrary(
        dirName = "jna",
        fileName = "libjnidispatch.jnilib",
        staticProperties = mapOf("jna.nounpack" to "true", "jna.nosys" to "true"),
        onResolved = { dir, _ -> System.setProperty("jna.boot.library.path", dir.absolutePath) },
    ),
    BundledNativeLibrary(
        dirName = "sqlite",
        fileName = "libsqlitejdbc.dylib",
        onResolved = { dir, file ->
            System.setProperty("org.sqlite.lib.path", dir.absolutePath)
            System.setProperty("org.sqlite.lib.name", file.name)
        },
    ),
    BundledNativeLibrary(
        dirName = "gojni",
        fileName = "libgojni.dylib",
        onResolved = { dir, file ->
            System.setProperty("ooni.gojni.boot.library.path", dir.absolutePath)
            System.setProperty("ooni.gojni.boot.library.name", file.name)
        },
    ),
    BundledNativeLibrary(
        dirName = "passport",
        fileName = "libuniffi_ooniprobe.dylib",
        onResolved = { dir, _ -> prependPathProperty("jna.library.path", dir) },
    ),
)

fun configureBundledNativeLibraries(
    resourcesDirectory: File? = System.getProperty("compose.application.resources.dir")?.let(::File),
): NativeRuntimeBootstrapResult {
    val architecture = macOsArchitecture() ?: return NativeRuntimeBootstrapResult(emptyList())
    val root = resourcesDirectory ?: return NativeRuntimeBootstrapResult(emptyList())
    val applied = bundledNativeLibraries.mapNotNull { library ->
        val directory = sequenceOf(architecture, "darwin")
            .map { File(root, "${library.dirName}/$it") }
            .firstOrNull(File::isDirectory)
            ?: return@mapNotNull null
        val file = File(directory, library.fileName).takeIf(File::isFile) ?: return@mapNotNull null
        val missing = library.additionalFiles.filterNot { File(directory, it).isFile }
        if (missing.isNotEmpty()) Logger.w("configureBundledNativeLibraries: ${library.dirName} missing $missing")
        library.staticProperties.forEach(System::setProperty)
        library.onResolved(directory, file)
        library.dirName
    }
    Logger.i("configureBundledNativeLibraries: applied $applied")
    return NativeRuntimeBootstrapResult(applied)
}

private fun macOsArchitecture(): String? {
    if (!System
            .getProperty("os.name")
            .orEmpty()
            .lowercase()
            .contains("mac")
    ) {
        return null
    }
    return when (System.getProperty("os.arch").orEmpty().lowercase()) {
        "aarch64", "arm64" -> "darwin-aarch64"
        "x86_64", "amd64" -> "darwin-x86-64"
        else -> "darwin"
    }
}

private fun prependPathProperty(
    name: String,
    directory: File,
) {
    val separator = File.pathSeparator
    val existing = System.getProperty(name).orEmpty()
    if (directory.absolutePath !in existing.split(separator)) {
        System.setProperty(name, if (existing.isEmpty()) directory.absolutePath else "${directory.absolutePath}$separator$existing")
    }
}
