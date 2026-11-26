package ooni.ios

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.internal.os.OperatingSystem
import java.io.File
import java.io.FileNotFoundException
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Refreshes a local Swift package under iosApp/Packages from the files published with a
 * GitHub release of [repository].
 *
 * Each release must publish every file in [files], including its `Package.swift`, or the
 * task fails. Files are downloaded to a temporary folder first, so a failed run leaves the
 * package unchanged.
 *
 * By default the task uses the release `v` + [releaseVersion], the version in
 * gradle/libs.versions.toml that Android and Desktop use. `--tag` selects another release,
 * e.g. `--tag=rolling`.
 *
 * macOS only: the packages are built by Xcode, and the task fails on other hosts.
 *
 * The `rolling` release is overwritten on every upstream build, so its checksums go stale;
 * run the task again when Xcode reports a checksum mismatch.
 */
abstract class UpdateIosSwiftPackageTask : DefaultTask() {
    /** GitHub repository, e.g. `ooni/probe-cli`. */
    @get:Input
    abstract val repository: Property<String>

    @get:Input
    @get:Option(option = "tag", description = "Release tag (default: v<version from libs.versions.toml>), e.g. rolling")
    abstract val tag: Property<String>

    /** Version from gradle/libs.versions.toml; the default tag is `v` + this version. */
    @get:Input
    abstract val releaseVersion: Property<String>

    /** Release asset name to destination path, relative to [packageDir]. */
    @get:Input
    abstract val files: MapProperty<String, String>

    @get:Internal
    abstract val packageDir: DirectoryProperty

    init {
        group = "ios"
        tag.convention(releaseVersion.map { "v$it" })
        doNotTrackState("Downloads from a GitHub release whose content can change")
    }

    @TaskAction
    fun update() {
        if (!OperatingSystem.current().isMacOsX) {
            throw GradleException("$name only runs on macOS: the iOS Swift packages are built with Xcode.")
        }
        val repo = repository.get()
        val releaseTag = tag.get()
        val baseUrl = "https://github.com/$repo/releases/download/$releaseTag"
        val assets = files.get()
        if ("Package.swift" !in assets) {
            throw GradleException("$name: `files` must include Package.swift")
        }

        val work = temporaryDir.apply {
            deleteRecursively()
            mkdirs()
        }
        assets.keys.forEach { asset ->
            val url = "$baseUrl/$asset"
            try {
                URI(url).toURL().openStream().use { input ->
                    Files.copy(input, File(work, asset).toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            } catch (e: FileNotFoundException) {
                throw GradleException(
                    "$repo $releaseTag has no $asset ($url). Every release used by the iOS app " +
                        "must publish ${assets.keys.joinToString(" and ")}.",
                    e,
                )
            }
        }
        if (!File(work, "Package.swift").readText().contains("import PackageDescription")) {
            throw GradleException("$baseUrl/Package.swift does not look like a Package.swift")
        }

        val pkg = packageDir.get().asFile
        assets.forEach { (asset, destination) ->
            val target = File(pkg, destination).apply { parentFile.mkdirs() }
            Files.move(
                File(work, asset).toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }

        logger.lifecycle("Updated $pkg from $repo $releaseTag.")
        logger.lifecycle("In Xcode: File > Packages > Reset Package Caches, then build.")
    }
}
