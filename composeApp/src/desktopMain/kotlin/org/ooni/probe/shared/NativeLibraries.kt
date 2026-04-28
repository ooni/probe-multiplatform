package org.ooni.probe.shared

import co.touchlab.kermit.Logger
import java.io.File

/**
 * Descriptor for a bundled native library that should be resolved from the
 * signed copy under `Contents/app/resources/<dirName>/<arch>/` instead of
 * letting its JAR extract an unsigned copy at runtime.
 *
 * @property dirName Sub-directory under `compose.application.resources.dir`.
 * @property fileName Expected file inside the resolved arch directory; the
 * entry is skipped if the file is absent (lets non-macOS or partial builds
 * silently no-op).
 * @property staticProperties System properties applied verbatim before
 * [onResolved] runs (e.g. JNA's `jna.nounpack` / `jna.nosys` toggles).
 * @property onResolved Library-specific hook invoked with the resolved
 * `(dir, file)` — sets dynamic system properties or `System.load`s the
 * library, depending on what the runtime loader expects.
 */
private data class BundledNativeLibrary(
    val dirName: String,
    val fileName: String,
    val staticProperties: Map<String, String> = emptyMap(),
    val onResolved: (dir: File, file: File) -> Unit,
)

/**
 * JNA's bootstrap dispatcher. Setting `jna.boot.library.path` plus the
 * `nounpack` / `nosys` toggles forces JNA's `Native` class to load the
 * pre-extracted, signed `libjnidispatch.jnilib` from the bundle.
 */
private val jnaLibrary = BundledNativeLibrary(
    dirName = "jna",
    fileName = "libjnidispatch.jnilib",
    staticProperties = mapOf(
        "jna.nounpack" to "true",
        "jna.nosys" to "true",
    ),
    onResolved = { dir, _ ->
        System.setProperty("jna.boot.library.path", dir.absolutePath)
    },
)

/**
 * sqlite-jdbc honours `org.sqlite.lib.path` + `org.sqlite.lib.name` to skip
 * its default extraction path; with both set it resolves the native library
 * via the absolute path when `org.sqlite.JDBC` class-loads.
 */
private val sqliteLibrary = BundledNativeLibrary(
    dirName = "sqlite",
    fileName = "libsqlitejdbc.dylib",
    onResolved = { dir, file ->
        System.setProperty("org.sqlite.lib.path", dir.absolutePath)
        System.setProperty("org.sqlite.lib.name", file.name)
    },
)

/**
 * gomobile-bind's `go.Seq.<clinit>` unconditionally calls
 * `go.NativeUtils.loadLibraryFromJar("/jniLibs/<arch>/libgojni.dylib")` on
 * macOS, which extracts an UNSIGNED copy of the dylib to the sandbox tmp
 * dir and `System.load`s it — rejected by the App Store hardened runtime
 * with *"library load disallowed by system policy"*.
 *
 * We shadow `go.NativeUtils` (see
 * `composeApp/src/desktopMain/kotlin/go/NativeUtils.kt`; composeApp's jar
 * precedes oonimkall on `app.classpath` so the shadow wins). That shadow
 * reads `ooni.gojni.boot.library.path` and `System.load`s the pre-signed
 * bundled dylib instead. We only set the property here; the actual load
 * happens lazily inside the shadow when `Seq.<clinit>` fires.
 */
private val goJniLibrary = BundledNativeLibrary(
    dirName = "gojni",
    fileName = "libgojni.dylib",
    onResolved = { dir, file ->
        // Mirror sqlite-jdbc's `org.sqlite.lib.path` (directory) +
        // `org.sqlite.lib.name` (filename) split. Our shadow class combines
        // them with `File(path, name)` to compute the absolute path passed
        // to `System.load`.
        System.setProperty("ooni.gojni.boot.library.path", dir.absolutePath)
        System.setProperty("ooni.gojni.boot.library.name", file.name)
    },
)

private val bundledNativeLibraries = listOf(jnaLibrary, sqliteLibrary, goJniLibrary)

/**
 * Points each native-library loader at its bundled, codesigned copy so the
 * Mac App Store sandbox + hardened runtime never see an unsigned extracted
 * dylib (which Gatekeeper rejects with `Apple could not verify ...`).
 *
 * The Gradle `extractMacOsNativeLibraries` task copies pre-built dylibs into
 * `Contents/app/resources/{jna,sqlite,gojni}/<arch>/`, the
 * `signBundledMacOsNativeLibraries` step codesigns them with our identity,
 * and this function points each runtime loader at the bundled copy so no
 * on-disk extraction is attempted. The Compose Desktop plugin strips the
 * `macos/` prefix from the staged resources, so the runtime layout is
 * `<resources>/<lib>/<arch>/…`.
 *
 * Must run before `dependencies` is first dereferenced — sqlite-jdbc reads
 * `org.sqlite.lib.*` when `org.sqlite.JDBC` class-loads, JNA reads
 * `jna.boot.library.path` when `Native` class-loads, and
 * `ooni.gojni.boot.library.path` must be set before `go.Seq.<clinit>`
 * triggers our shadowed `go.NativeUtils.loadLibraryFromJar`.
 *
 * No-op on non-macOS hosts or when the bundled files are absent.
 */
internal fun configureBundledNativeLibraries() {
    val applied = bundledNativeLibraries.mapNotNull { lib ->
        val dir = macOsBundledLibraryDir(lib.dirName) ?: return@mapNotNull null
        val file = File(dir, lib.fileName).takeIf { it.isFile } ?: return@mapNotNull null
        lib.staticProperties.forEach { (key, value) -> System.setProperty(key, value) }
        lib.onResolved(dir, file)
        lib.dirName
    }
    Logger.i("configureBundledNativeLibraries: applied $applied")
}

/** Returns the macOS arch-specific subdirectory name, or `null` off-macOS. */
private val macOsArchSubdir: String?
    get() {
        val osName = System.getProperty("os.name").orEmpty().lowercase()
        if (!osName.contains("mac")) return null
        return when (System.getProperty("os.arch").orEmpty().lowercase()) {
            "aarch64", "arm64" -> "darwin-aarch64"
            "x86_64", "amd64" -> "darwin-x86-64"
            else -> "darwin"
        }
    }

/**
 * Resolves `<compose.application.resources.dir>/<libRoot>/<arch>` (or the
 * fallback `darwin/` directory), or `null` if neither exists. Returns
 * `null` when the host is not macOS or `compose.application.resources.dir`
 * is unset (e.g. running from an IDE without packaged resources).
 */
private fun macOsBundledLibraryDir(libRoot: String): File? {
    val arch = macOsArchSubdir ?: return null
    val resourcesDir = System.getProperty("compose.application.resources.dir") ?: return null
    val root = File(resourcesDir, libRoot)
    return sequenceOf(arch, "darwin")
        .map { File(root, it) }
        .firstOrNull { it.isDirectory }
}
