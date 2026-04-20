import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem
import java.io.File
import java.util.Properties

/**
 * Describes the desktop distribution channel. Replaces the old boolean
 * "is this an app store build" with a typed value so capability checks can
 * live on the enum and new channels can slot in without more booleans.
 *
 * Selected at configuration time via `-PdesktopDistribution=<cliValue>`.
 */
enum class Distribution(val cliValue: String) {
    Direct("direct"),
    MacAppStore("mac-appstore"),
    MicrosoftStore("ms-store"),
    ;

    val supportsSelfUpdate: Boolean get() = this == Direct
    val requiresSandbox: Boolean get() = this == MacAppStore
    val bundlesSparkle: Boolean get() = this == Direct
    val bundlesWinSparkle: Boolean get() = this == Direct
    val isAppStore: Boolean get() = this != Direct
}

/**
 * Resolve the active distribution from the `-PdesktopDistribution` property.
 *
 * Defaults to [Distribution.Direct]. Accepts the legacy value `appstore`
 * as a deprecated alias: it resolves to [Distribution.MacAppStore] on macOS
 * hosts and [Distribution.MicrosoftStore] on Windows hosts, mirroring how
 * Compose Desktop picks the current-OS package format.
 */
fun Project.distribution(): Distribution {
    val raw = findProperty("desktopDistribution")?.toString()?.trim()?.lowercase()
        ?: return Distribution.Direct

    Distribution.values().firstOrNull { it.cliValue == raw }?.let { return it }

    if (raw == "appstore") {
        val os = OperatingSystem.current()
        logger.warn(
            "-PdesktopDistribution=appstore is deprecated. Use 'mac-appstore' or 'ms-store'.",
        )
        return when {
            os.isMacOsX -> Distribution.MacAppStore
            os.isWindows -> Distribution.MicrosoftStore
            else -> error(
                "-PdesktopDistribution=appstore is ambiguous on this host; " +
                    "pass mac-appstore or ms-store explicitly.",
            )
        }
    }

    error(
        "Unknown desktopDistribution=$raw; expected one of " +
            Distribution.values().joinToString { it.cliValue } + " (or legacy 'appstore').",
    )
}

/**
 * Relative glob patterns for every desktop resource bundled solely to drive
 * self-updates (Sparkle/WinSparkle and the `updatebridge` JNI glue). Paths
 * are relative to `composeApp/src/desktopMain/resources/`.
 *
 * The same list is consumed twice: once by the resource-prep Sync to exclude
 * these files from store builds, and once by the `verifyStoreBundle` task to
 * confirm nothing matching them ended up inside a produced `.pkg` / `.exe`.
 * Adding a new update-related resource only requires extending this list.
 */
val desktopUpdateResourcePatterns: List<String> = listOf(
    "windows/WinSparkle.*",
    "windows/libwinpthread-1.dll",
    "windows/updatebridge.*",
    "windows/include/**",
    "macos/libupdatebridge.dylib",
)

/**
 * Filename fragments that must not appear in a store-distributed bundle.
 * Matched case-insensitively by [verifyStoreBundle] against the list of
 * files inside `.pkg` / `.exe` payloads. Kept separate from
 * [desktopUpdateResourcePatterns] because those are build-time glob paths
 * while these are substring markers we expect inside packaged archives
 * (framework dirs, signed helper tools, etc.).
 */
val forbiddenStoreBundleMarkers: List<String> = listOf(
    "Sparkle",
    "WinSparkle",
    "updatebridge",
    "libwinpthread",
)

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
fun Project.isDebugTaskRequested(): Boolean {
    val isTaskDebug = gradle.startParameter.taskRequests
        .flatMap { it.args }
        .any {
            it.contains("Debug") ||
                it.contains("test", ignoreCase = true) ||
                (it.contains("run", ignoreCase = true) && !it.contains("Release", ignoreCase = true))
        }
    logger.info("isTaskDebug=$isTaskDebug")

    if (isTaskDebug) return true

    return try {
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())
        logger.info("forceDebug=${properties["forceDebug"]}")
        properties["forceDebug"] == true || properties["forceDebug"] == "true"
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

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
