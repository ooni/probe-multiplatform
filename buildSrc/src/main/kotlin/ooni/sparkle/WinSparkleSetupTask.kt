package ooni.sparkle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

abstract class WinSparkleSetupTask : DefaultTask() {
    @get:Input
    abstract val winSparkleVersion: Property<String>

    @get:OutputDirectory
    abstract val destDir: DirectoryProperty

    @TaskAction
    @Suppress("NewApi")
    fun run() {
        val versionStr = winSparkleVersion.get()
        val url = "https://github.com/vslavik/winsparkle/releases/download/v$versionStr/WinSparkle-$versionStr.zip"
        val dest = destDir.get().asFile

        dest.mkdirs()

        val versionMarker = File(dest, "WinSparkle.version")
        val dllTarget = File(dest, "WinSparkle.dll")
        if (dllTarget.exists() && versionMarker.exists() && versionMarker.readText() == versionStr) {
            project.logger.lifecycle("WinSparkle.dll $versionStr already prepared at ${dest.absolutePath}; skipping")
            return
        }

        val buildDirFile = project.layout.buildDirectory.get().asFile
        val archiveFile = File(buildDirFile, "WinSparkle-$versionStr.zip")
        val extractTmpDir = File(buildDirFile, "winsparkle/extracted-$versionStr").apply { mkdirs() }

        if (!archiveFile.exists()) {
            project.logger.lifecycle("Downloading WinSparkle $versionStr from $url …")
            URI(url).toURL().openStream().use { input ->
                Files.copy(input, archiveFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } else {
            project.logger.lifecycle("Using cached archive ${archiveFile.absolutePath}")
        }

        project.logger.lifecycle("Extracting archive to ${extractTmpDir.absolutePath} …")
        ZipFile(archiveFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val outFile = File(extractTmpDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        Files.copy(input, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        }

        val releaseDir = File(extractTmpDir, "WinSparkle-$versionStr/x64/Release")
        if (!releaseDir.exists() || !releaseDir.isDirectory) {
            throw GradleException("Expected 'Release' directory in the extracted archive, but it was not found")
        }

        project.logger.lifecycle("Copying files from ${releaseDir.absolutePath} to ${dest.absolutePath} …")
        releaseDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                val targetFile = File(dest, file.name)
                Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }

        versionMarker.writeText(versionStr)
        project.logger.lifecycle("WinSparkle.dll is ready at ${dest.absolutePath}")
    }
}
