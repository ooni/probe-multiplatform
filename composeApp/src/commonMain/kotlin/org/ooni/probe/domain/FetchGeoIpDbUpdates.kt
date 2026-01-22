package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.ooni.engine.Engine
import org.ooni.engine.Engine.MkException
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.GetBytesException
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class FetchGeoIpDbUpdates(
    private val downloadFile: suspend (url: String, absoluteTargetPath: String) -> Result<Path, GetBytesException>,
    private val cacheDir: String,
    private val engineHttpDo: suspend (method: String, url: String, taskOrigin: TaskOrigin) -> Result<String?, Engine.MkException>,
    private val preferencesRepository: PreferenceRepository,
    private val json: Json,
    private val fileSystem: FileSystem,
    private val backgroundContext: CoroutineContext,
) {
    companion object {
        private const val GEOIP_DB_VERSION_DEFAULT: String = "20250801"
        private const val GEOIP_DB_REPO: String = "ooni/historical-geoip"
    }

    suspend operator fun invoke(): Result<Path?, MkException> {
        // Check if we've already checked today
        val lastCheckMillis = preferencesRepository.getValueByKey(SettingsKey.MMDB_LAST_CHECK).first() as? Long
        if (lastCheckMillis != null) {
            if (Clock.System.now() - Instant.fromEpochMilliseconds(lastCheckMillis) < 1.days) {
                // Less than a day has passed, skip the check
                return Success(null)
            }
        }

        return getLatestEngineVersion()
            .flatMap { version ->
                val latest: String =
                    version ?: return@flatMap Failure(MkException(IllegalStateException("Failed to fetch latest GeoIP DB release")))
                val (isLatest, latestVersion) = isGeoIpDbLatest(latest)
                if (isLatest) {
                    // Update last check time even when already at latest version
                    preferencesRepository.setValueByKey(
                        SettingsKey.MMDB_LAST_CHECK,
                        Clock.System.now().toEpochMilliseconds(),
                    )
                    Success(null)
                } else {
                    val url = buildGeoIpDbUrl(latestVersion)
                    val target = run {
                        val cacheDirPath = cacheDir.toPath()
                        if (!fileSystem.exists(cacheDirPath)) {
                            fileSystem.createDirectories(cacheDirPath)
                        }
                        cacheDirPath.resolve("$latestVersion.mmdb").toString()
                    }
                    downloadFile(url, target)
                        .flatMap { downloadedPath ->
                            withContext(backgroundContext) {
                                // Cleanup other mmdb files other than the latest
                                cleanupOldMmdbFiles(latestVersion)
                            }
                            preferencesRepository.setValueByKey(
                                SettingsKey.MMDB_VERSION,
                                latestVersion,
                            )
                            preferencesRepository.setValueByKey(
                                SettingsKey.MMDB_LAST_CHECK,
                                Clock.System.now().toEpochMilliseconds(),
                            )
                            Success(downloadedPath)
                        }.mapError { downloadError ->
                            MkException(downloadError)
                        }
                }
            }
    }

    /**
     * Compare latest and current version integers and return pair of latest state and actual version number
     * @return Pair<Boolean, String> where the first element is true if the DB is the latest,
     * the second is the current version and the third is the latest version.
     */
    private suspend fun isGeoIpDbLatest(latestVersion: String): Pair<Boolean, String> {
        val currentGeoIpDbVersion: String =
            (
                preferencesRepository.getValueByKey(SettingsKey.MMDB_VERSION).first()
                    ?: GEOIP_DB_VERSION_DEFAULT
            ) as String

        return Pair(
            normalize(currentGeoIpDbVersion) >= normalize(latestVersion),
            latestVersion,
        )
    }

    private suspend fun getLatestEngineVersion(): Result<String?, MkException> {
        val url = "https://api.github.com/repos/${GEOIP_DB_REPO}/releases/latest"

        return engineHttpDo("GET", url, TaskOrigin.OoniRun).map { payload ->
            payload?.let {
                try {
                    json.decodeFromString<GhRelease>(payload).tag
                } catch (e: SerializationException) {
                    Logger.e(e) { "Failed to decode release info" }
                    null
                } catch (e: IllegalArgumentException) {
                    Logger.e(e) { "Failed to decode  release info" }
                    null
                }
            }
        }
    }

    private fun buildGeoIpDbUrl(version: String): String =
        "https://github.com/${GEOIP_DB_REPO}/releases/download/$version/$version-ip2country_as.mmdb"

    private fun normalize(tag: String): Int = tag.removePrefix("v").trim().toInt()

    /**
     * Delete all .mmdb files in the cache directory except for the specified version.
     * @param keepVersion The version of the mmdb file to keep (e.g., "20250801")
     */
    private fun cleanupOldMmdbFiles(keepVersion: String) {
        try {
            val cacheDirPath = cacheDir.toPath()
            if (!fileSystem.exists(cacheDirPath)) {
                return
            }

            val files = fileSystem.list(cacheDirPath)
            val keepFileName = "$keepVersion.mmdb"

            files.forEach { filePath ->
                val fileName = filePath.name
                if (fileName.endsWith(".mmdb") && fileName != keepFileName) {
                    try {
                        fileSystem.delete(filePath)
                        Logger.d { "Deleted old MMDB file: $fileName" }
                    } catch (e: Exception) {
                        Logger.e(e) { "Failed to delete old MMDB file: $fileName" }
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e(e) { "Failed to cleanup old MMDB files" }
        }
    }

    @Serializable
    data class GhRelease(
        @SerialName("tag_name") val tag: String,
    )
}
