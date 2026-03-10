package org.ooni.probe.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath
import org.ooni.engine.models.EnginePreferences
import org.ooni.engine.models.TaskLogLevel
import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.ProxyOption
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository
import kotlin.time.Duration.Companion.seconds

class GetEnginePreferences(
    private val preferencesRepository: PreferenceRepository,
    private val getProxyOption: () -> Flow<ProxyOption>,
    private val cacheDir: String,
) {
    suspend operator fun invoke() =
        EnginePreferences(
            enabledWebCategories = getEnabledCategories(),
            taskLogLevel = if (getValueForKey(SettingsKey.DEBUG_LOGS) == true) {
                TaskLogLevel.Debug
            } else {
                TaskLogLevel.Info
            },
            uploadResults = getValueForKey(SettingsKey.UPLOAD_RESULTS) == true,
            maxRuntime = if (getValueForKey(SettingsKey.MAX_RUNTIME_ENABLED) == true) {
                (getValueForKey(SettingsKey.MAX_RUNTIME) as? Int)?.seconds
            } else {
                null
            },
            proxy = getProxyOption().first().value,
            geoipDbPath = getGeoIpDbPath(),
        )

    private suspend fun getEnabledCategories(): List<String> {
        val categoriesValues = preferencesRepository
            .allSettings(WebConnectivityCategory.entries.mapNotNull { it.settingsKey })
            .first()
        return WebConnectivityCategory.entries
            .filter { it.settingsKey != null && categoriesValues[it.settingsKey] == true }
            .map { it.code }
    }

    private suspend fun getValueForKey(settingsKey: SettingsKey) = preferencesRepository.getValueByKey(settingsKey).first()

    private suspend fun getGeoIpDbPath(): String? {
        if (!OrganizationConfig.shouldFetchGeoIpDbUpdates) return null
        val version = getValueForKey(SettingsKey.MMDB_VERSION) as? String ?: return null
        return cacheDir.toPath().resolve("$version.mmdb").toString()
    }
}
