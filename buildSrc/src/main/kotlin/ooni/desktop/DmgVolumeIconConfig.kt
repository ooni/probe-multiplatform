package ooni.desktop

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.ListProperty

/**
 * Wires `--icon <icon>` into the jpackage invocation that produces the DMG
 * volume image. jpackage uses this icon both for the .app bundle and for the
 * mounted DMG volume, but the Compose Desktop DSL only configures the bundle
 * icon — the DMG-specific volume icon needs to be passed via `freeArgs`.
 *
 * Targets the Compose Desktop DMG packaging tasks (`packageDmg` /
 * `packageReleaseDmg`). We avoid a hard compile-time reference to
 * `org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask`
 * because pulling the compose-gradle-plugin into buildSrc clashes with the
 * root project's version-catalog plugin resolution. Instead, match by name
 * and access `freeArgs` reflectively — the property's signature is part of
 * Compose Desktop's stable public API.
 *
 * @param iconFilePath absolute path to the `.icns` file used as DMG icon.
 */
fun Project.configureDmgVolumeIcon(iconFilePath: String) {
    DMG_PACKAGE_TASK_NAMES.forEach { taskName ->
        tasks.matching { it.name == taskName }.configureEach {
            logger.lifecycle("$name: setting DMG volume icon → $iconFilePath")
            freeArgsOf(this).addAll("--icon", iconFilePath)
        }
    }
}

internal val DMG_PACKAGE_TASK_NAMES = setOf("packageDmg", "packageReleaseDmg")

@Suppress("UNCHECKED_CAST")
internal fun freeArgsOf(task: Task): ListProperty<String> =
    task.javaClass.getMethod("getFreeArgs").invoke(task) as ListProperty<String>
