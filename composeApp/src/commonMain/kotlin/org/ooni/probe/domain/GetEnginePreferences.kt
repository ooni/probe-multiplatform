package org.ooni.probe.domain

import kotlinx.coroutines.flow.first
import org.ooni.engine.models.EnginePreferences
import org.ooni.engine.models.TaskLogLevel
import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.data.models.ProxySettings
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository
import kotlin.time.Duration.Companion.seconds

class GetEnginePreferences(
    private val preferencesRepository: PreferenceRepository,
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
            maxRuntime = (getValueForKey(SettingsKey.MAX_RUNTIME) as? Int)?.seconds,
            proxy = ProxySettings.newProxySettings(
                protocol = getValueForKey(SettingsKey.PROXY_PROTOCOL) as? String,
                hostname = getValueForKey(SettingsKey.PROXY_HOSTNAME) as? String,
                port = getValueForKey(SettingsKey.PROXY_PORT) as? String,
            ).getProxyString(),
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
}
