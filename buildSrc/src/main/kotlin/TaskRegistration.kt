import org.gradle.api.Project
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import java.io.File
import kotlin.let
import ooni.sparkle.SetupSparkleTask

/**
 * Registers all custom tasks for the project
 */
fun Project.registerTasks(config: AppConfig) {
    registerAndroidTasks(config)
    registerDesktopTasks()
    registerResourceTasks(config)
    registerSparkleTask()
    configureTaskDependencies()
}

private fun Project.registerAndroidTasks(config: AppConfig) {
    tasks.register("runDebug", Exec::class) {
        group = "ooni"
        description = "Clean, install and run the debug variant"
        dependsOn("clean", "installFullDebug")
        commandLine(
            "adb",
            "shell",
            "am",
            "start",
            "-n",
            "${config.appId}.dev/org.ooni.probe.MainActivity",
        )
    }
}

private fun Project.registerDesktopTasks() {
    tasks.register("makeLibrary", Exec::class) {
        group = "ooni"
        description = "Build native libraries (NetworkTypeFinder and UpdateBridge)"
        dependsOn("setupSparkle")
        workingDir = file("src/desktopMain")
        commandLine = listOf("make", "all")
        doFirst {
            println("🔨 Building native libraries...")
        }
        doLast {
            println("✅ Native libraries built successfully")
        }
    }

    tasks.register("cleanLibrary", Exec::class) {
        group = "ooni"
        description = "Clean native library build artifacts"
        workingDir = file("src/desktopMain")
        commandLine = listOf("make", "clean")
    }
}

private fun Project.registerSparkleTask() {
    tasks.register("setupSparkle", SetupSparkleTask::class) {
        group = "setup"
        description =
            "Downloads Sparkle and extracts Sparkle.framework to the destination directory"
        sparkleVersion.set(providers.gradleProperty("sparkleVersion").orElse("2.8.0"))
        destDir.set(
            providers.gradleProperty("sparkleExtractDir")
                .map { layout.projectDirectory.dir(it) }
                .orElse(layout.buildDirectory.dir("processedResources/desktop/main/macos/")),
        )
    }

    tasks.register("createOONIDistributable") {
        group = "build"
        description = "Processes the createDistributable output (e.g., zip it or sign it)"
        dependsOn("createDistributable")
        doLast {
            // Get the task reference
            val distributableTask = tasks.named("createDistributable").get()


            // Access the output directory path
            val outputDir: File = distributableTask.outputs.files.singleFile

            // find a .app in output dir
            val appDirs = outputDir.listFiles { file ->
                file.isDirectory && file.name.endsWith(".app")
            } ?: emptyArray()

            if (appDirs.isNotEmpty()) {
                project.logger.lifecycle("Found .app: ${appDirs.joinToString { it.absolutePath }}")
            } else {
                project.logger.error("No .app found in ${outputDir.absolutePath}")
            }

            val sparkleFramework =
                layout.buildDirectory.dir("processedResources/desktop/main/macos/Sparkle.framework")
                    .get()
            val appSparkleLocation = appDirs.first().resolve("Contents/app/resources")

            project.logger.lifecycle("Sparkle.framework location: ${sparkleFramework.asFile.absolutePath}")
            project.logger.lifecycle("Desired Sparkle.framework location: ${appSparkleLocation.absolutePath}")

            project.exec {
                commandLine(
                    "cp",
                    "-a",
                    sparkleFramework.asFile.absolutePath,
                    appSparkleLocation.absolutePath
                )
            }


            project.logger.lifecycle("Distributable output directory: ${outputDir.absolutePath}")

        }
    }
}

private fun Project.registerResourceTasks(config: AppConfig) {
    tasks.register("copyBrandingToCommonResources") {
        group = "ooni"
        description = "Copy branding resources to common resources directory"
        doLast {
            val projectDir = project.projectDir.absolutePath
            copyRecursive(
                from = File(projectDir, "src/${config.folder}/res"),
                to = File(projectDir, "src/commonMain/res"),
            )
            copyRecursive(
                from = File(projectDir, "src/${config.folder}/composeResources"),
                to = File(projectDir, "src/commonMain/composeResources"),
            )
        }
    }

    tasks.register("cleanCopiedCommonResourcesToFlavor") {
        group = "ooni"
        description = "Clean copied common resources from flavor directories"
        doLast {
            val projectDir = project.projectDir.absolutePath

            deleteFilesFromGitIgnore("$projectDir/src/commonMain/res")
            deleteFilesFromGitIgnore("$projectDir/src/commonMain/resources")
            deleteFilesFromGitIgnore("$projectDir/src/commonMain/composeResources")
        }
    }
}

private fun Project.configureTaskDependencies() {
    // Configure existing tasks with dependencies after evaluation
    afterEvaluate {
        tasks.findByName("compileKotlinDesktop")?.dependsOn?.add("makeLibrary")

        tasks.findByName("preBuild")?.dependsOn("copyBrandingToCommonResources")

        tasks.findByName("clean")
            ?.dependsOn("copyBrandingToCommonResources", "cleanCopiedCommonResourcesToFlavor")

        // Ensure Sparkle.framework is prepared before packaging desktop apps
        val sparkleConsumers = setOf(
            "createDistributable",
            "packageDistributionForCurrentOS",
            "packageDmg"
        )
        tasks.matching { it.name in sparkleConsumers }.configureEach {
            dependsOn("setupSparkle")
        }

        // Prefer running setupSparkle after desktop resource processing to avoid overwrites
        tasks.findByName("setupSparkle")?.let { setup ->
            val desktopRes = tasks.matching {
                it.name.contains(
                    "processResources",
                    ignoreCase = true
                ) && it.name.contains("desktop", ignoreCase = true)
            }
            setup.mustRunAfter(desktopRes)
        }
    }
}

/**
 * Configures existing tasks with OONI-specific settings
 */
fun Project.configureTasks() {
    configureJavaExecTasks()
}

private fun Project.configureJavaExecTasks() {
    tasks.withType<JavaExec> {
        systemProperty(
            "java.library.path",
            "$projectDir/src/desktopMain/resources/macos" +
                File.pathSeparator +
                "$projectDir/src/desktopMain/resources/windows" +
                File.pathSeparator +
                "$projectDir/src/desktopMain/resources/linux" +
                File.pathSeparator +
                System.getProperty("java.library.path"),
        )

        // Get desktop updates public key from project properties
        project.findProperty("desktopUpdatesPublicKey")?.let { key ->
            systemProperty("desktopUpdatesPublicKey", key)
        }
    }
}


/**
 * Configure tasks to delete files from .gitignore.
 * This version matches the original implementation from the build script.
 */
fun deleteFilesFromGitIgnore(folderPath: String) {
    val destinationFile = File(folderPath)
    destinationFile.listFiles()?.forEach { folder ->
        folder.listFiles()?.forEach { file ->
            if (file.name == ".gitignore") {
                file
                    .readText()
                    .lines()
                    .forEach { line ->
                        if (line.isNotEmpty()) {
                            println("Removing $line")
                            File(folder, line).deleteRecursively()
                        }
                    }.also {
                        file.delete()
                    }
            }
        }
    }
}
