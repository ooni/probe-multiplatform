package ooni.sparkle

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class GenerateSparkleAppCastTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Input
    abstract val sparkleVersion: Property<String>

    @get:InputFile
    abstract val edKeyFile: RegularFileProperty

    @get:Input
    abstract val appCastFile: Property<String>

    @get:Input
    abstract val downloadUrlPrefix: Property<String>

    @TaskAction
    fun run() {
        val sparkleTool = sparkleVersion
            .map { project.layout.buildDirectory.file("sparkle/extracted-$it/bin/generate_appcast") }
            .get()

        val dmgFile = project.file("build/compose/binaries/main/dmg")

        val command = listOf(
            sparkleTool.get().asFile.absolutePath,
            "-o",
            appCastFile.get(),
            "--ed-key-file",
            edKeyFile.get().asFile.absolutePath,
            "--download-url-prefix",
            downloadUrlPrefix.get(),
            dmgFile.absolutePath
        )

        project.logger.lifecycle("Running Sparkle generate_appcast with command: ${command.joinToString(" ")}")

        execOperations.exec {
            commandLine(command)
        }.assertNormalExitValue()
    }
}
