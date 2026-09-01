@file:JvmName("NativeUtils")
@file:Suppress("unused")

package go

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.jvm.Throws

private const val BOOT_LIBRARY_PATH_PROPERTY = "ooni.gojni.boot.library.path"
private const val BOOT_LIBRARY_NAME_PROPERTY = "ooni.gojni.boot.library.name"

private val loaded = AtomicBoolean(false)

@Throws(IOException::class)
fun loadLibraryFromJar(path: String) {
    val bundledDir = System.getProperty(BOOT_LIBRARY_PATH_PROPERTY)
    val bundledName = System.getProperty(BOOT_LIBRARY_NAME_PROPERTY)
    if (bundledDir != null && bundledName != null) {
        if (loaded.compareAndSet(false, true)) System.load(File(bundledDir, bundledName).absolutePath)
        return
    }
    val name = path.substringAfterLast('/')
    val temporaryDirectory = Files.createTempDirectory("nativeutils").apply { toFile().deleteOnExit() }
    val output = temporaryDirectory.resolve(name).apply { toFile().deleteOnExit() }
    val input = object {}.javaClass.getResourceAsStream(path) ?: throw FileNotFoundException(path)
    input.use { Files.copy(it, output) }
    if (loaded.compareAndSet(false, true)) System.load(output.toAbsolutePath().toString())
}
