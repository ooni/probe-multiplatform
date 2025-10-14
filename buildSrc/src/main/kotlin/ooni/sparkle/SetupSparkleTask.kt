package ooni.sparkle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

abstract class SetupSparkleTask : DefaultTask() {

    @get:Input
    abstract val sparkleVersion: Property<String>

    @get:OutputDirectory
    abstract val destDir: DirectoryProperty

    @TaskAction
    fun run() {
        val versionStr = sparkleVersion.get()
        val url =
            "https://github.com/sparkle-project/Sparkle/releases/download/$versionStr/Sparkle-$versionStr.tar.xz"
        val dest = destDir.get().asFile

        dest.mkdirs()

        val versionMarker = File(dest, "Sparkle.version")
        val targetFramework = File(dest, "Sparkle.framework")
        if (targetFramework.exists() && versionMarker.exists() && versionMarker.readText() == versionStr) {
            project.logger.lifecycle("Sparkle.framework $versionStr already prepared at ${dest.absolutePath}; skipping")
            return
        }

        val buildDirFile = project.layout.buildDirectory.get().asFile
        // Download the archive directly into the module's build directory
        val archiveFile = File(buildDirFile, "Sparkle-$versionStr.tar.xz")
        val extractTmpDir = File(buildDirFile, "sparkle/extracted-$versionStr").apply { mkdirs() }

        if (!archiveFile.exists()) {
            project.logger.lifecycle("Downloading Sparkle $versionStr from $url …")
            URL(url).openStream().use { input ->
                Files.copy(input, archiveFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } else {
            project.logger.lifecycle("Using cached archive ${archiveFile.absolutePath}")
        }

        project.logger.lifecycle("Extracting archive to ${extractTmpDir.absolutePath} …")
        project.exec {
            commandLine("tar", "-xJf", archiveFile.absolutePath, "-C", extractTmpDir.absolutePath)
        }

        val frameworkDir = File(extractTmpDir, "Sparkle.framework")
        if (!frameworkDir.exists()) {
            throw GradleException("Expected Sparkle.framework in the extracted archive, but it was not found")
        }

        // Replace any existing framework to avoid mixing versions
        if (targetFramework.exists()) {
            targetFramework.deleteRecursively()
        }

        project.logger.lifecycle("Copying Sparkle.framework from ${frameworkDir.absolutePath} to ${dest.absolutePath} …")
        project.exec {
            commandLine("cp", "-a", frameworkDir.absolutePath, dest.absolutePath)
        }
        /*project.copy {
            from(frameworkDir)
            into(dest)
        }*/

        versionMarker.writeText(versionStr)
        project.logger.lifecycle("Sparkle.framework is ready at ${dest.absolutePath}")
    }
}
