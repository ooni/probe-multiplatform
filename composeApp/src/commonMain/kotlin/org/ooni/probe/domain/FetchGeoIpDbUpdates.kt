package org.ooni.probe.domain

import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.Path
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.engine_mmdb_version
import org.jetbrains.compose.resources.getString
import org.ooni.engine.Engine
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository
import kotlin.time.Clock

class FetchGeoIpDbUpdates(
    private val downloadFile: suspend (url: String, absoluteTargetPath: String) -> Path,
    private val cacheDir: String,
    private val engineHttpDo: suspend (method: String, url: String, taskOrigin: TaskOrigin) -> Result<String?, Engine.MkException>,
    private val preferencesRepository: PreferenceRepository,
    private val json: Json,
) {
    suspend operator fun invoke(): Result<Path?, Engine.MkException> =
        try {
            when (val versionRes = getLatestEngineVersion()) {
                is Failure -> Failure(versionRes.reason)
                is Success -> {
                    val (isLatest, _, latestVersion) = isGeoIpDbLatest(versionRes.value)
                    if (isLatest) {
                        Success(null)
                    } else {
                        val versionName = latestVersion
                        val url = buildGeoIpDbUrl(versionName)
                        val target = "$cacheDir/$versionName.mmdb"

                        downloadFile(url, target).let {
                            preferencesRepository.setValueByKey(SettingsKey.MMDB_VERSION, versionName)
                            preferencesRepository.setValueByKey(SettingsKey.MMDB_LAST_CHECK, Clock.System.now().toEpochMilliseconds())
                            Success(it)
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Failure(t as? Engine.MkException ?: Engine.MkException(t))
        }

    /**
     * Compare latest and current version integers and return pair of latest state and actual version number
     * @return Triple<Boolean, String, String> where the first element is true if the DB is the latest,
     * the second is the current version and the third is the latest version.
     */
    private suspend fun isGeoIpDbLatest(latestVersion: String): Triple<Boolean, String, String> {
        val currentGeoIpDbVersion: String =
            (preferencesRepository.getValueByKey(SettingsKey.MMDB_VERSION).first() ?: getString(Res.string.engine_mmdb_version)) as String

        return Triple(normalize(currentGeoIpDbVersion) >= normalize(latestVersion), currentGeoIpDbVersion, latestVersion)
    }

    private suspend fun getLatestEngineVersion(): Result<String, Engine.MkException> {
        val url = "https://api.0.github.com/repos/aanorbel/oomplt-mmdb/releases/latest"

        return engineHttpDo("GET", url, TaskOrigin.OoniRun).map { payload ->
            val jsonStr = payload ?: throw Engine.MkException(Throwable("Empty body"))
            json.decodeFromString(GhRelease.serializer(), jsonStr).tag
        }
    }

    private fun buildGeoIpDbUrl(version: String): String =
        "https://github.com/aanorbel/oomplt-mmdb/releases/download/$version/$version-ip2country_as.mmdb"

    private fun normalize(tag: String): Int = tag.removePrefix("v").trim().toInt()

    @Serializable
    data class GhRelease(
        @SerialName("tag_name") val tag: String,
    )
}
