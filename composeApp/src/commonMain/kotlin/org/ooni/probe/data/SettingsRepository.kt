package org.ooni.probe.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {
    fun allSettings(keys: List<Preferences.Key<*>>): Flow<Map<String, Any?>> =
        dataStore.data.map {
            keys.map { key -> key.name to it[key] }.toMap()
        }

    suspend fun <T> getValueByKey(
        key: Preferences.Key<T>,
        defaultValue: T,
    ): T {
        return dataStore.data.map { it[key] }.firstOrNull() ?: defaultValue
    }
}
