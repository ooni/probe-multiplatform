package ooni.desktop

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import java.io.File

/**
 * Re-encodes jpackage's default zlib-compressed DMG into UDBZ (LZMA), which
 * cuts our distributable size noticeably for the Direct macOS channel.
 *
 * jpackage emits `${packageName}-${packageVersion}.dmg` in `destinationDir`;
 * after the task runs, this `doLast` hook calls `hdiutil convert -format UDBZ`
 * into a temporary file, deletes the original, and renames the temp file back
 * to the canonical name so downstream consumers (Sparkle appcast generation,
 * publish steps) see the same path.
 *
 * Targets `packageDmg` / `packageReleaseDmg` by name and reads the typed
 * jpackage properties reflectively so buildSrc doesn't need to depend on
 * the compose-gradle-plugin (see comment in [configureDmgVolumeIcon]).
 */
fun Project.configureDmgUdbzConversion() {
    DMG_PACKAGE_TASK_NAMES.forEach { taskName ->
        tasks.matching { it.name == taskName }.configureEach {
            doLast {
                val destDir = destinationDirOf(this).get().asFile
                val pkgName = packageNameOf(this).get()
                val pkgVersion = packageVersionOf(this).get()
                val dmgFile = File(destDir, "$pkgName-$pkgVersion.dmg")
                if (!dmgFile.exists()) {
                    logger.error("$name: DMG file not found: ${dmgFile.absolutePath}")
                    throw GradleException("Expected DMG file not found: ${dmgFile.absolutePath}")
                }

                val originalSizeMb = dmgFile.length() / (1024 * 1024)
                logger.lifecycle(
                    "$name: converting ${dmgFile.name} (${originalSizeMb} MB, zlib) → UDBZ/LZMA …",
                )
                val tempDmg = File(destDir, "temp-$pkgName-$pkgVersion.dmg")
                try {
                    project.providers.exec {
                        commandLine(
                            "hdiutil",
                            "convert",
                            dmgFile.absolutePath,
                            "-format",
                            "UDBZ",
                            "-o",
                            tempDmg.absolutePath,
                        )
                    }.result.get().assertNormalExitValue()

                    if (!tempDmg.exists()) {
                        throw GradleException(
                            "DMG conversion succeeded but output file not found: ${tempDmg.absolutePath}",
                        )
                    }
                    if (!dmgFile.delete()) {
                        throw GradleException("Failed to delete original DMG file: ${dmgFile.absolutePath}")
                    }
                    if (!tempDmg.renameTo(dmgFile)) {
                        throw GradleException(
                            "Failed to rename converted DMG from ${tempDmg.absolutePath} to ${dmgFile.absolutePath}",
                        )
                    }
                    val newSizeMb = dmgFile.length() / (1024 * 1024)
                    val savedMb = originalSizeMb - newSizeMb
                    logger.lifecycle(
                        "$name: UDBZ conversion done — ${dmgFile.name} now ${newSizeMb} MB " +
                            "(saved ${savedMb} MB / ${if (originalSizeMb > 0) savedMb * 100 / originalSizeMb else 0}%)",
                    )
                } catch (e: Exception) {
                    if (tempDmg.exists()) tempDmg.delete()
                    throw GradleException("Failed to convert DMG to UDBZ format: ${e.message}", e)
                }
            }
        }
    }
}

private fun destinationDirOf(task: Task): DirectoryProperty =
    task.javaClass.getMethod("getDestinationDir").invoke(task) as DirectoryProperty

@Suppress("UNCHECKED_CAST")
private fun packageNameOf(task: Task): Property<String> =
    task.javaClass.getMethod("getPackageName").invoke(task) as Property<String>

@Suppress("UNCHECKED_CAST")
private fun packageVersionOf(task: Task): Property<String> =
    task.javaClass.getMethod("getPackageVersion").invoke(task) as Property<String>
