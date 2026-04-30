package ooni.desktop

import desktopUpdateResourcePatterns
import distribution
import ooni.jna.ExtractMacOsNativeLibrariesTask
import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

/**
 * Registers the `prepareDesktopResources` Sync task that stages
 * `src/desktopMain/resources/` into a per-distribution build dir, layering in
 * the macOS native libs extracted by [ExtractMacOsNativeLibrariesTask] under
 * `macos/<lib>/<arch>/`. When the active distribution doesn't bundle a
 * self-updater (Sparkle/WinSparkle/updatebridge), the matching glob patterns
 * from [desktopUpdateResourcePatterns] are excluded from the staged output.
 *
 * The Compose Desktop plugin strips the `macos/` prefix on darwin targets,
 * so files staged at `macos/<lib>/<arch>/...` end up at
 * `<.app>/Contents/app/resources/<lib>/<arch>/...` inside the bundle.
 */
fun Project.registerPrepareDesktopResourcesTask(): TaskProvider<Sync> {
    val dist = distribution()
    val sourceDir = layout.projectDirectory.dir("src/desktopMain/resources/")
    val outputDir = layout.buildDirectory.dir("tmp/desktop-resources-${dist.name.lowercase()}")
    val stripUpdaters = !dist.bundlesSparkle || !dist.bundlesWinSparkle

    return tasks.register<Sync>("prepareDesktopResources") {
        from(sourceDir)
        from(
            tasks.named<ExtractMacOsNativeLibrariesTask>("extractMacOsNativeLibraries")
                .map { it.outputDir },
        ) {
            into("macos")
        }
        into(outputDir)
        if (stripUpdaters) {
            desktopUpdateResourcePatterns.forEach { exclude(it) }
        }
        doFirst {
            logger.lifecycle(
                "prepareDesktopResources: staging desktop resources for distribution=${dist.name} " +
                    "into ${outputDir.get().asFile.absolutePath}",
            )
            if (stripUpdaters) {
                logger.lifecycle(
                    "prepareDesktopResources: stripping updater patterns " +
                        desktopUpdateResourcePatterns.joinToString(prefix = "[", postfix = "]"),
                )
            }
        }
    }
}
