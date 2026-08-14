package org.ooni.probe.core

import co.touchlab.kermit.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private data class BundledNativeLibrary(
    val dirName: String,
    val fileName: String,
    val additionalFiles: List<String> = emptyList(),
    val staticProperties: Map<String, String> = emptyMap(),
    // Location of the library inside the dependency jars, given a `darwin-<arch>` value.
    val classpathResource: (arch: String) -> String,
    val onResolved: (dir: File, file: File) -> Unit,
)

private val bundledNativeLibraries = listOf(
    BundledNativeLibrary(
        dirName = "jna",
        fileName = "libjnidispatch.jnilib",
        staticProperties = mapOf("jna.nounpack" to "true", "jna.nosys" to "true"),
        classpathResource = { arch -> "com/sun/jna/$arch/libjnidispatch.jnilib" },
        onResolved = { dir, _ -> System.setProperty("jna.boot.library.path", dir.absolutePath) },
    ),
    BundledNativeLibrary(
        dirName = "sqlite",
        fileName = "libsqlitejdbc.dylib",
        classpathResource = { arch ->
            "org/sqlite/native/Mac/${if (arch == "darwin-aarch64") "aarch64" else "x86_64"}/libsqlitejdbc.dylib"
        },
        onResolved = { dir, file ->
            System.setProperty("org.sqlite.lib.path", dir.absolutePath)
            System.setProperty("org.sqlite.lib.name", file.name)
        },
    ),
    BundledNativeLibrary(
        dirName = "gojni",
        fileName = "libgojni.dylib",
        classpathResource = { arch ->
            "jniLibs/${if (arch == "darwin-aarch64") "arm64" else "amd64"}/libgojni.dylib"
        },
        onResolved = { dir, file ->
            System.setProperty("ooni.gojni.boot.library.path", dir.absolutePath)
            System.setProperty("ooni.gojni.boot.library.name", file.name)
        },
    ),
    BundledNativeLibrary(
        dirName = "passport",
        fileName = "libuniffi_ooniprobe.dylib",
        classpathResource = { "darwin-universal/libuniffi_ooniprobe.dylib" },
        onResolved = { dir, _ -> prependPathProperty("jna.library.path", dir) },
    ),
)

fun configureBundledNativeLibraries(
    resourcesDirectory: File? = System.getProperty("compose.application.resources.dir")?.let(::File),
): NativeRuntimeBootstrapResult {
    val architecture = macOsArchitecture() ?: return NativeRuntimeBootstrapResult(emptyList())
    val root = resourcesDirectory ?: extractClasspathNativeLibraries(architecture)
        ?: return NativeRuntimeBootstrapResult(emptyList())
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

private var classpathExtractDir: File? = null

// No resources directory (e.g. the CLI distribution): stage the libraries bundled inside the
// dependency jars into a temp dir laid out as `<dirName>/<arch>/<fileName>`, so the regular
// resolution above can wire the same loader properties against it.
private fun extractClasspathNativeLibraries(architecture: String): File? {
    classpathExtractDir?.let { return it }
    val classLoader = object {}.javaClass.classLoader
    val tempRoot = Files.createTempDirectory("ooni-bundled-natives").toFile().apply { deleteOnExit() }
    var extractedAny = false
    bundledNativeLibraries.forEach { library ->
        val input = classLoader.getResourceAsStream(library.classpathResource(architecture))
            ?: return@forEach
        val directory = File(tempRoot, "${library.dirName}/$architecture").apply { mkdirs() }
        val file = File(directory, library.fileName).apply { deleteOnExit() }
        input.use { Files.copy(it, file.toPath(), StandardCopyOption.REPLACE_EXISTING) }
        extractedAny = true
    }
    if (!extractedAny) return null
    Logger.i("configureBundledNativeLibraries: staged classpath natives in ${tempRoot.absolutePath}")
    return tempRoot.also { classpathExtractDir = it }
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
