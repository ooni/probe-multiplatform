package org.ooni.probe.shared

import co.touchlab.kermit.Logger
import dev.dirs.ProjectDirectories
import okio.Path.Companion.toPath
import kotlinx.coroutines.withContext
import java.io.File

class LegacyDirectoryManager(
    os: DesktopOS,
) {
    private val oldProjectDirectories = ProjectDirectories.fromPath("OONI Probe")

    private val legacyPaths = when (os) {
        DesktopOS.Mac -> listOf(
            oldProjectDirectories.cacheDir,
            oldProjectDirectories.dataDir,
            oldProjectDirectories.configDir,
            oldProjectDirectories.dataLocalDir,
            oldProjectDirectories.preferenceDir,
            System.getProperty("user.home") + "/Library/LaunchAgents/org.ooni.probe-desktop.plist",
        )

        DesktopOS.Windows -> listOf(
            oldProjectDirectories.cacheDir
                .toPath()
                .parent
                .toString(),
            oldProjectDirectories.dataDir
                .toPath()
                .parent
                .toString(),
        )

        else -> emptyList()
    }

    fun hasLegacyDirectories(): Boolean =
        legacyPaths.any { path ->
            val file = File(path)
            file.exists() && file.isDirectory && file.listFiles().any()
        }

    suspend fun cleanupLegacyDirectories(): Boolean {
        Logger.i { "Starting cleanup of legacy directories..." }

        val results = withContext(kotlinx.coroutines.Dispatchers.IO) {
            legacyPaths.map { dirPath ->
                Logger.i { "Attempting to clean up legacy path: $dirPath" }
                deletePath(dirPath).also {
                    if (it) {
                        Logger.i { "Successfully cleaned up legacy path: $dirPath" }
                    } else {
                        Logger.w { "Failed to clean up legacy path: $dirPath" }
                    }
                }
            }
        }
        Logger.i { "Legacy directory cleanup process finished." }
        return results.all { it }
    }

    private fun deletePath(path: String): Boolean {
        val target = File(path)
        return try {
            if (target.exists()) {
                if (target.isDirectory) {
                    target.deleteRecursively()
                } else {
                    target.delete()
                }
            } else {
                Logger.i { "Path does not exist, no cleanup needed: $path" }
                true
            }
        } catch (e: SecurityException) {
            Logger.e(e) { "SecurityException: Permission denied while trying to delete ${target.absolutePath}" }
            false
        } catch (e: Exception) {
            Logger.e(e) { "Exception while trying to delete ${target.absolutePath}: ${e.message}" }
            false
        }
    }
}
