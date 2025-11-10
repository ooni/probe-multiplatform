package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.Path
import org.ooni.engine.Engine
import org.ooni.engine.Engine.MkException
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.GetBytesException
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository
import kotlin.time.Clock

class FetchGeoIpDbUpdates(
    private val downloadFile: suspend (url: String, absoluteTargetPath: String) -> Result<Path, GetBytesException>,
    private val cacheDir: String,
    private val engineHttpDo: suspend (method: String, url: String, taskOrigin: TaskOrigin) -> Result<String?, Engine.MkException>,
    private val preferencesRepository: PreferenceRepository,
    private val json: Json,
) {
    companion object {
        private const val GEOIP_DB_VERSION_DEFAULT: String = "20250801"
        private const val GEOIP_DB_REPO: String = "aanorbel/oomplt-mmdb"
    }

    suspend operator fun invoke(): Result<Path?, MkException> =
        getLatestEngineVersion()
            .flatMap { version ->
                val (isLatest, latestVersion) = isGeoIpDbLatest(version)
                if (isLatest) {
                    Success(null)
                } else {
                    val url = buildGeoIpDbUrl(latestVersion)
                    val target = "$cacheDir/$latestVersion.mmdb"

                    downloadFile(url, target)
                        .flatMap { downloadedPath ->
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

    private suspend fun getLatestEngineVersion(): Result<String, MkException> {
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
            } ?: throw MkException(Throwable("Failed to fetch latest version"))
        }
    }

    private fun buildGeoIpDbUrl(version: String): String =
        "https://github.com/${GEOIP_DB_REPO}/releases/download/$version/$version-ip2country_as.mmdb"

    private fun normalize(tag: String): Int = tag.removePrefix("v").trim().toInt()

    @Serializable
    data class GhRelease(
        @SerialName("tag_name") val tag: String,
    )
}
