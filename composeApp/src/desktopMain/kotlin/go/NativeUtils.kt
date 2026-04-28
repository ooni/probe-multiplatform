@file:JvmName("NativeUtils")
@file:Suppress("unused") // entry point is resolved by the JVM via classpath shadowing

package go

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.jvm.Throws

/**
 * Shadow of `go.NativeUtils` from `oonimkall-*-darwin.jar` (gomobile-bind).
 *
 * Upstream `loadLibraryFromJar` extracts the JNI dylib from the JAR resource
 * to a tmp file under the sandbox container and `System.load`s that copy.
 * On the macOS App Store hardened runtime the extracted file carries no
 * code signature so dyld rejects it with *"library load disallowed by system
 * policy"*, propagating an `ExceptionInInitializerError` from
 * `go.Seq.<clinit>` and permanently poisoning `oonimkall.SessionConfig`.
 *
 * `composeApp-desktop-*.jar` precedes `oonimkall-*-darwin.jar` in the
 * jpackage launcher's `app.classpath`, so this class wins JVM class
 * resolution and replaces the upstream behaviour. Class name, package
 * (`go`), method signature, and `throws IOException` clause must stay
 * byte-for-byte compatible with the upstream — `go.Seq.<clinit>` calls it
 * via `invokestatic` and any drift produces `NoSuchMethodError` at link
 * time.
 *
 * Contract (mirrors the JNA / sqlite-jdbc property pattern in
 * [org.ooni.probe.shared.configureBundledNativeLibraries]):
 *  - When `ooni.gojni.boot.library.path` (directory) and
 *    `ooni.gojni.boot.library.name` (filename) are both set, `System.load`
 *    `<path>/<name>`. Idempotent across calls and threads via [loaded].
 *  - Otherwise (Linux / Windows / IDE runs without packaged resources, or
 *    the bundled signed dylib is missing) fall back to the upstream
 *    extract-to-tmp behaviour so non-App-Store environments keep working.
 *  - Never swallow `UnsatisfiedLinkError`: failing fast with a stack trace
 *    pointing at the bundled path is the whole point of this shadow.
 *    Wrapping it would reproduce the silent-failure pathology that
 *    motivated the refactor.
 */
private const val BOOT_LIBRARY_PATH_PROPERTY = "ooni.gojni.boot.library.path"
private const val BOOT_LIBRARY_NAME_PROPERTY = "ooni.gojni.boot.library.name"

private val loaded = AtomicBoolean(false)

@Throws(IOException::class)
fun loadLibraryFromJar(path: String) {
    val bundledDir = System.getProperty(BOOT_LIBRARY_PATH_PROPERTY)
    val bundledName = System.getProperty(BOOT_LIBRARY_NAME_PROPERTY)
    if (bundledDir != null && bundledName != null) {
        if (loaded.compareAndSet(false, true)) {
            System.load(File(bundledDir, bundledName).absolutePath)
        }
        return
    }
    fallbackExtractAndLoad(path)
}

@Throws(IOException::class)
private fun fallbackExtractAndLoad(resourcePath: String) {
    val name = resourcePath.substringAfterLast('/')
    val tmpDir = Files.createTempDirectory("nativeutils").apply { toFile().deleteOnExit() }
    val out = tmpDir.resolve(name).apply { toFile().deleteOnExit() }
    val stream = object {}.javaClass.getResourceAsStream(resourcePath)
        ?: throw FileNotFoundException(resourcePath)
    stream.use { Files.copy(it, out) }
    if (loaded.compareAndSet(false, true)) {
        System.load(out.toAbsolutePath().toString())
    }
}
