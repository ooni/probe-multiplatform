package core.settings

import kotlinx.coroutines.flow.Flow

class SettingsStoreImpl(
    private val settingsManager: SettingsManager,
) : SettingsStore {
    override fun clearAll() {
        return settingsManager.clearAllSettings()
    }
    override fun saveProbeCredentials(value: String) {
        return settingsManager.setString(key = SettingsManager.PROBE_CREDENTIALS, value = value)
    }
    override fun getProbeCredentials(): Flow<String?> {
        return settingsManager.getString(key = SettingsManager.PROBE_CREDENTIALS)
    }
}