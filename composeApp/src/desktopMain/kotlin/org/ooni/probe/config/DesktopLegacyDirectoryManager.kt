package org.ooni.probe.config

import co.touchlab.kermit.Logger
import dev.dirs.ProjectDirectories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import org.ooni.probe.background.runCommand
import org.ooni.probe.shared.DesktopOS
import java.io.File
import kotlin.coroutines.CoroutineContext

class DesktopLegacyDirectoryManager(
    os: DesktopOS,
    private val backgroundContext: CoroutineContext = Dispatchers.IO,
) : LegacyDirectoryManager {
    private val cleanUpDone = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
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
            oldProjectDirectories.cacheDir,
            oldProjectDirectories.dataDir,
        ).map { it.toPath().parent.toString() }

        else -> emptyList()
    }

    override val isCleanUpRequired: Flow<Boolean>
        get() = cleanUpDone.onStart { emit(Unit) }.map { hasLegacyDirectories() }

    private suspend fun hasLegacyDirectories(): Boolean =
        withContext(backgroundContext) {
            legacyPaths.any { path ->
                val file = File(path)
                file.exists() && file.isDirectory && file.listFiles().any()
            }
        }

    override suspend fun cleanUp(): Boolean =
        withContext(backgroundContext) {
            Logger.i { "Starting cleanup of legacy directories..." }

            uninstallLegacyApp()

            val results = legacyPaths.map { dirPath ->
                Logger.i { "Attempting to clean up legacy path: $dirPath" }
                deletePath(dirPath).also {
                    if (it) {
                        Logger.i { "Successfully cleaned up legacy path: $dirPath" }
                    } else {
                        Logger.w { "Failed to clean up legacy path: $dirPath" }
                    }
                }
            }

            Logger.i { "Legacy directory cleanup process finished." }
            cleanUpDone.tryEmit(Unit)
            return@withContext results.all { it }
        }

    suspend fun uninstallLegacyApp() {
        val localAppData = oldProjectDirectories.cacheDir
            .replace("cache", "")
            .replace("OONI Probe", "")
            .replace("\\\\", "\\")
        val legacyInstallDir = "$localAppData\\Programs\\ooniprobe-desktop"
        // find exe with word uninstall in name in the folder `legacyInstallDir`
        val legacyUnInstallExe = File(legacyInstallDir).walkTopDown().firstOrNull {
            it.isFile && it.name.contains("uninstall", ignoreCase = true)
        }
        if (legacyUnInstallExe != null) {
            Logger.i { "Found legacy uninstall executable: ${legacyUnInstallExe.absolutePath}" }
            runCommand(arrayOf(legacyUnInstallExe.absolutePath, "/S"))
        }
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
