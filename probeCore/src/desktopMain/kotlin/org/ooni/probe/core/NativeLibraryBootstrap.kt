package org.ooni.probe.core

import co.touchlab.kermit.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private data class BundledNativeLibrary(
    val dirName: String,
    // Location of the library inside the dependency jars, given a `<os>-<arch>` value
    // (`darwin`/`linux`/`win32` combined with `aarch64`/`x86-64`), e.g. `linux-x86-64`.
    // Returns null if this library isn't published for the given platform.
    val classpathResource: (arch: String) -> String?,
    val additionalFiles: List<String> = emptyList(),
    val staticProperties: Map<String, String> = emptyMap(),
    val onResolved: (dir: File, file: File) -> Unit,
)

// All four bundled libraries follow the same filename convention across platforms:
// `lib<name>.dylib` on macOS, `lib<name>.so` on Linux, and `<name>.dll` (no "lib" prefix) on
// Windows - verified against the real JNA, sqlite-jdbc, oonimkall, and passport jars.
private fun nativeLibFileName(
    name: String,
    arch: String,
) = when (arch.substringBefore('-')) {
    "darwin" -> "lib$name.dylib"
    "linux" -> "lib$name.so"
    "win32" -> "$name.dll"
    else -> error("Unsupported architecture: $arch")
}

private val bundledNativeLibraries = listOf(
    BundledNativeLibrary(
        dirName = "jna",
        staticProperties = mapOf("jna.nounpack" to "true", "jna.nosys" to "true"),
        classpathResource = { arch -> "com/sun/jna/$arch/${nativeLibFileName("jnidispatch", arch)}" },
        onResolved = { dir, _ -> System.setProperty("jna.boot.library.path", dir.absolutePath) },
    ),
    BundledNativeLibrary(
        dirName = "sqlite",
        classpathResource = { arch ->
            val (os, cpu) = arch.split("-", limit = 2)
            val sqliteOs = when (os) {
                "darwin" -> "Mac"
                "linux" -> "Linux"
                "win32" -> "Windows"
                else -> error("Unsupported architecture: $arch")
            }
            "org/sqlite/native/$sqliteOs/${cpu.replace("-", "_")}/${nativeLibFileName("sqlitejdbc", arch)}"
        },
        onResolved = { dir, file ->
            System.setProperty("org.sqlite.lib.path", dir.absolutePath)
            System.setProperty("org.sqlite.lib.name", file.name)
        },
    ),
    BundledNativeLibrary(
        dirName = "gojni",
        classpathResource = { arch ->
            val (os, cpu) = arch.split("-", limit = 2)
            val abi = if (cpu == "aarch64") "arm64" else "amd64"
            "jniLibs/$abi/${nativeLibFileName("gojni", arch)}"
        },
        onResolved = { dir, file ->
            System.setProperty("ooni.gojni.boot.library.path", dir.absolutePath)
            System.setProperty("ooni.gojni.boot.library.name", file.name)
        },
    ),
    BundledNativeLibrary(
        dirName = "passport",
        // Published per-OS: macOS ships one universal binary, Linux/Windows ship per-arch ones
        // (and Windows only ships x86-64).
        classpathResource = { arch ->
            when (arch.substringBefore('-')) {
                "darwin" -> "darwin-universal/${nativeLibFileName("uniffi_ooniprobe", arch)}"
                "linux" -> "$arch/${nativeLibFileName("uniffi_ooniprobe", arch)}"
                "win32" -> "win32-x86-64/${nativeLibFileName("uniffi_ooniprobe", arch)}"
                else -> null
            }
        },
        onResolved = { dir, _ -> prependPathProperty("jna.library.path", dir) },
    ),
)

fun configureBundledNativeLibraries(
    resourcesDirectory: File? = System.getProperty("compose.application.resources.dir")?.let(::File),
): NativeRuntimeBootstrapResult {
    val architecture = hostArchitecture() ?: return NativeRuntimeBootstrapResult(emptyList())
    val osFamily = architecture.substringBefore('-')
    val root = resourcesDirectory ?: extractClasspathNativeLibraries(architecture)
        ?: return NativeRuntimeBootstrapResult(emptyList())
    val applied = bundledNativeLibraries.mapNotNull { library ->
        val directory = sequenceOf(architecture, osFamily)
            .map { File(root, "${library.dirName}/$it") }
            .firstOrNull(File::isDirectory)
            ?: return@mapNotNull null
        val resource = library.classpathResource(architecture) ?: return@mapNotNull null
        val file = File(directory, resource.substringAfterLast('/')).takeIf(File::isFile) ?: return@mapNotNull null
        val missing = library.additionalFiles.filterNot { File(directory, it).isFile }
        if (missing.isNotEmpty()) Logger.w("configureBundledNativeLibraries: ${library.dirName} missing $missing")
        library.staticProperties.forEach(System::setProperty)
        library.onResolved(directory, file)
        library.dirName
    }
    Logger.i("configureBundledNativeLibraries: applied $applied")
    return NativeRuntimeBootstrapResult(applied)
}

private fun hostArchitecture(): String? {
    val osName = System.getProperty("os.name").orEmpty().lowercase()
    val osFamily = when {
        osName.contains("mac") -> "darwin"
        osName.contains("linux") -> "linux"
        osName.contains("windows") -> "win32"
        else -> return null
    }
    return when (System.getProperty("os.arch").orEmpty().lowercase()) {
        "aarch64", "arm64" -> "$osFamily-aarch64"
        "x86_64", "amd64" -> "$osFamily-x86-64"
        else -> osFamily
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
        val resource = library.classpathResource(architecture) ?: return@forEach
        val input = classLoader.getResourceAsStream(resource) ?: return@forEach
        val directory = File(tempRoot, "${library.dirName}/$architecture").apply { mkdirs() }
        val file = File(directory, resource.substringAfterLast('/')).apply { deleteOnExit() }
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
