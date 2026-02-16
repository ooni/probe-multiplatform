import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem
import java.io.File

/**
 * Check if F-Droid build task is requested.
 */
fun Project.isFdroidTaskRequested(): Boolean =
    gradle.startParameter.taskRequests
        .flatMap { it.args }
        .any { it.contains("Fdroid") }

/**
 * Check if Debug build task is requested.
 */
fun Project.isDebugTaskRequested(): Boolean =
    gradle.startParameter.taskRequests
        .flatMap { it.args }
        .any { it.contains("Debug") }

/**
 * Get the appropriate JavaFX suffix for the current OS and architecture.
 */
fun getJavaFxSuffix(): String {
    val os = OperatingSystem.current()
    val arch = System.getProperty("os.arch")
    return when {
        os.isMacOsX -> if (arch == "aarch64") "mac-aarch64" else "mac"
        os.isWindows -> "win"
        os.isLinux -> if (arch == "aarch64") "linux-aarch64" else "linux"
        else -> throw IllegalStateException("Unknown OS: $os")
    }
}

/**
 * Get the appropriate oonimkall suffix for the current OS.
 */
fun oonimkallVersionSuffix(): String {
    val os = OperatingSystem.current()
    return when {
        os.isMacOsX -> "darwin"
        os.isWindows -> "windows"
        os.isLinux -> "linux"
        else -> throw IllegalStateException("Unknown OS: $os")
    }
}

/**
 * Add a line to .gitignore if it doesn't already exist.
 */
fun ignoreCopiedFileIfNotIgnored(
    gitignorePath: String,
    lineToAdd: String,
) {
    val file = File(gitignorePath)
    if (!file.exists()) {
        file.createNewFile()
    }

    val fileContents = file.readText()

    if (!fileContents.contains(lineToAdd)) {
        file.appendText("\n$lineToAdd")
    }
}

/**
 * Copy files from one directory to another recursively.
 *
 * @param from The source directory.
 * @param to The destination directory.
 */
fun copyRecursive(
    from: File,
    to: File,
) {
    if (!from.exists()) {
        println("Source directory does not exist: $from")
        return
    }
    from.listFiles()?.forEach { file ->
        if (file.name != ".DS_Store") {
            if (file.isDirectory) {
                val newDir = File(to, file.name)
                newDir.mkdir()
                copyRecursive(file, newDir)
            } else {
                val destinationFile = File(to, file.name)
                if (destinationFile.exists()) {
                    destinationFile.delete()
                }
                if (!destinationFile.parentFile.exists()) {
                    destinationFile.parentFile.mkdirs()
                }
                file.copyTo(destinationFile).also {
                    ignoreCopiedFileIfNotIgnored(
                        to.absolutePath + "/.gitignore",
                        it.name,
                    )
                }
            }
        }
    }
}
