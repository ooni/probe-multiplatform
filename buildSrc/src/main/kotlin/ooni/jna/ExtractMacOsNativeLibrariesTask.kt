package ooni.jna

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import java.io.File
import java.util.zip.ZipFile

/**
 * Extracts bundled native libraries (JNA dispatcher, sqlite-jdbc, gojni) from
 * their runtime jars so they can be staged inside the macOS .app and signed
 * with our Team ID. Mac App Store builds run with the app sandbox + hardened
 * runtime, which enforces library validation even when
 * `com.apple.security.cs.disable-library-validation` is set — so the default
 * behaviour of these libraries (unpacking dylibs into ~/Library/Caches/... at
 * first use) is rejected by Gatekeeper with "Apple could not verify ... is
 * free of malware". Bundling pre-signed copies inside Contents/app/resources/
 * avoids the on-disk extraction entirely.
 */
abstract class ExtractMacOsNativeLibrariesTask : DefaultTask() {

    @get:InputFiles
    abstract val runtimeClasspath: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun extract() {
        if (!OperatingSystem.current().isMacOsX) {
            logger.lifecycle("extractMacOsNativeLibraries: skipping on non-macOS host")
            return
        }
        val out = outputDir.get().asFile
        logger.lifecycle("extractMacOsNativeLibraries: staging darwin native libs into ${out.absolutePath}")
        out.deleteRecursively()
        out.mkdirs()

        val classpathFiles = runtimeClasspath.files
        var totalExtracted = 0
        EXTRACTION_PLANS.forEach { plan ->
            val jar = classpathFiles.firstOrNull { plan.jarPattern.matches(it.name) }
            if (jar == null) {
                if (plan.required) {
                    throw GradleException(
                        "ExtractMacOsNativeLibrariesTask: could not find jar matching " +
                            "${plan.jarPattern.pattern} on runtime classpath",
                    )
                }
                logger.lifecycle(
                    "extractMacOsNativeLibraries: optional jar " +
                        "${plan.jarPattern.pattern} not on classpath; skipping",
                )
                return@forEach
            }
            var extracted = 0
            ZipFile(jar).use { zip ->
                plan.entries.forEach { (entry, relativeOut) ->
                    val ze = zip.getEntry(entry) ?: return@forEach
                    val dst = File(out, relativeOut).apply { parentFile.mkdirs() }
                    zip.getInputStream(ze).use { input ->
                        dst.outputStream().use { output -> input.copyTo(output) }
                    }
                    extracted++
                }
            }
            if (plan.required && extracted == 0) {
                throw GradleException(
                    "ExtractMacOsNativeLibrariesTask: no expected darwin entries " +
                        "found in ${jar.name}",
                )
            }
            logger.lifecycle("extractMacOsNativeLibraries: extracted $extracted entr(ies) from ${jar.name}")
            totalExtracted += extracted
        }
        logger.lifecycle("extractMacOsNativeLibraries: done — $totalExtracted file(s) staged")
    }

    private data class Plan(
        val jarPattern: Regex,
        val entries: List<Pair<String, String>>,
        val required: Boolean,
    )

    private companion object {
        val EXTRACTION_PLANS: List<Plan> = listOf(
            // JNA dispatcher — required on macOS for the JNA-based platform
            // helpers (autolaunch, dark mode detector, etc).
            Plan(
                jarPattern = Regex("""^jna-\d.*\.jar$"""),
                entries = listOf(
                    "com/sun/jna/darwin-aarch64/libjnidispatch.jnilib" to "jna/darwin-aarch64/libjnidispatch.jnilib",
                    "com/sun/jna/darwin-x86-64/libjnidispatch.jnilib" to "jna/darwin-x86-64/libjnidispatch.jnilib",
                    "com/sun/jna/darwin/libjnidispatch.jnilib" to "jna/darwin/libjnidispatch.jnilib",
                ),
                required = true,
            ),
            // sqlite-jdbc native — required for the SQLDelight desktop driver.
            Plan(
                jarPattern = Regex("""^sqlite-jdbc-.*\.jar$"""),
                entries = listOf(
                    "org/sqlite/native/Mac/aarch64/libsqlitejdbc.dylib" to "sqlite/darwin-aarch64/libsqlitejdbc.dylib",
                    "org/sqlite/native/Mac/x86_64/libsqlitejdbc.dylib" to "sqlite/darwin-x86-64/libsqlitejdbc.dylib",
                ),
                required = true,
            ),
            // oonimkall (gojni) — only present on the macOS classifier of
            // oonimkall, so this jar is only on the classpath for desktop
            // macOS builds. Skip silently if not found.
            Plan(
                jarPattern = Regex("""^oonimkall-.*-darwin\.jar$"""),
                entries = listOf(
                    "jniLibs/arm64/libgojni.dylib" to "gojni/darwin-aarch64/libgojni.dylib",
                    "jniLibs/amd64/libgojni.dylib" to "gojni/darwin-x86-64/libgojni.dylib",
                ),
                required = false,
            ),
        )
    }
}
