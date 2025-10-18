package ooni.appimage

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class PackageAppImageTask : DefaultTask() {

    @get:Inject
    abstract val execOperations: ExecOperations

    /**
     * The create-appimage.sh script file
     */
    @get:InputFile
    abstract val scriptFile: RegularFileProperty

    /**
     * The project root directory
     */
    @get:InputDirectory
    abstract val projectDir: DirectoryProperty

    /**
     * The expected output AppImage file
     */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        group = "distribution"
        description = "Creates an AppImage for OONI Probe desktop application"

        // Set default script location
        scriptFile.convention(
            project.layout.projectDirectory.file("scripts/create-appimage.sh")
        )

        // Set default project directory
        projectDir.convention(project.layout.projectDirectory)
    }

    @TaskAction
    fun createAppImage() {
        val script = scriptFile.get().asFile

        if (!script.exists()) {
            throw IllegalStateException("AppImage creation script not found at: ${script.absolutePath}")
        }

        if (!script.canExecute()) {
            logger.warn("Making create-appimage.sh executable...")
            script.setExecutable(true)
        }

        val args = mutableListOf<String>(script.absolutePath)

        logger.lifecycle("Running: ${args.joinToString(" ")}")

        execOperations.exec {
            workingDir = projectDir.get().asFile
            commandLine = args
            standardOutput = System.out
            errorOutput = System.err
        }.assertNormalExitValue()

        // Verify output was created
        val outDirFile = outputDir.get().asFile
        val output = outDirFile.listFiles()?.firstOrNull { it.isFile && it.name.startsWith("OONI-Probe") && it.name.endsWith(".AppImage") }
        if (output != null) {
            logger.lifecycle("✓ AppImage created successfully: ${output.absolutePath}")
            logger.lifecycle("  Size: ${output.length() / (1024 * 1024)} MB")
        } else {
            logger.warn("Output AppImage not found in expected directory: ${outDirFile.absolutePath}")
            logger.warn("Check the appimage-workspace directory for the output")
        }
    }
}
