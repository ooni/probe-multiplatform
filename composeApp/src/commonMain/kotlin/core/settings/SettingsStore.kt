package core.settings

import kotlinx.coroutines.flow.Flow

interface SettingsStore {
    fun clearAll()
    fun saveProbeCredentials(value: String)
    fun getProbeCredentials(): Flow<String?>
}