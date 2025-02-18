package org.ooni.probe.domain

import kotlinx.coroutines.flow.first
import org.ooni.probe.data.models.ProxySettings
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.data.repositories.PreferenceRepository

class GetProxySettings(
    private val preferencesRepository: PreferenceRepository,
) {
    suspend operator fun invoke() =
        ProxySettings.newProxySettings(
            protocol = getValueForKey(SettingsKey.PROXY_PROTOCOL) as? String,
            hostname = getValueForKey(SettingsKey.PROXY_HOSTNAME) as? String,
            port = getValueForKey(SettingsKey.PROXY_PORT) as? Int,
        )

    private suspend fun getValueForKey(settingsKey: SettingsKey) = preferencesRepository.getValueByKey(settingsKey).first()
}
