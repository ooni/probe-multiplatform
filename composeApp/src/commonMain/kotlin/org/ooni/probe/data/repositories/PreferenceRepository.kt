package org.ooni.probe.data.repositories

import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.ooni.probe.data.models.SettingsKey

sealed class PreferenceKey<T>(val preferenceKey: Preferences.Key<T>) {
    class IntKey(preferenceKey: Preferences.Key<Int>) : PreferenceKey<Int>(preferenceKey)

    class StringKey(preferenceKey: Preferences.Key<String>) : PreferenceKey<String>(preferenceKey)

    class BooleanKey(preferenceKey: Preferences.Key<Boolean>) : PreferenceKey<Boolean>(preferenceKey)

    class FloatKey(preferenceKey: Preferences.Key<Float>) : PreferenceKey<Float>(preferenceKey)

    class LongKey(preferenceKey: Preferences.Key<Long>) : PreferenceKey<Long>(preferenceKey)
}

class PreferenceRepository(
    private val dataStore: DataStore<Preferences>,
) {
    /**
     * This function is used to resolve the preference key for a given test.
     * The preference key is the name of the test prefixed with the [prefix] of the descriptor
     * and suffixed with "_autorun" if [autoRun] is true.
     *
     * Example: the preference key for the test "web_connectivity" in the
     * descriptor "websites" is "websites_web_connectivity".
     * If [autoRun] is true, the preference key is "websites_web_connectivity_autorun".
     *
     * @param name The name of the preference
     * @param prefix The prefix of the preference
     * @param autoRun If the preference is for auto run
     * @return The preference key
     */
    @VisibleForTesting
    fun getPreferenceKey(
        name: String,
        prefix: String? = null,
        autoRun: Boolean = false,
    ): String {
        return "${prefix?.let { "${it}_" } ?: ""}$name${if (autoRun) "_autorun" else ""}"
    }

    private fun preferenceKeyFromSettingsKey(
        key: SettingsKey,
        prefix: String? = null,
        autoRun: Boolean = false,
    ): PreferenceKey<*> {
        val preferenceKey = getPreferenceKey(name = key.value, prefix = prefix, autoRun = autoRun)
        return when (key) {
            SettingsKey.MAX_RUNTIME,
            SettingsKey.PROXY_PORT,
            -> PreferenceKey.IntKey(intPreferencesKey(preferenceKey))
            SettingsKey.PROXY_HOSTNAME,
            SettingsKey.PROXY_PROTOCOL,
            SettingsKey.LANGUAGE_SETTING,
            -> PreferenceKey.StringKey(stringPreferencesKey(preferenceKey))
            else -> PreferenceKey.BooleanKey(booleanPreferencesKey(preferenceKey))
        }
    }

    fun allSettings(
        keys: List<SettingsKey>,
        prefix: String? = null,
        autoRun: Boolean = false,
    ): Flow<Map<SettingsKey, Any?>> =
        dataStore.data.map {
            keys.map { key -> key to it[preferenceKeyFromSettingsKey(key, prefix, autoRun).preferenceKey] }.toMap()
        }

    fun getValueByKey(key: SettingsKey): Flow<Any?> {
        return dataStore.data.map {
            when (val preferenceKey = preferenceKeyFromSettingsKey(key)) {
                is PreferenceKey.IntKey -> it[preferenceKey.preferenceKey]
                is PreferenceKey.StringKey -> it[preferenceKey.preferenceKey]
                is PreferenceKey.BooleanKey -> it[preferenceKey.preferenceKey]
                is PreferenceKey.FloatKey -> it[preferenceKey.preferenceKey]
                is PreferenceKey.LongKey -> it[preferenceKey.preferenceKey]
            }
        }
    }

    suspend fun <T> setValueByKey(
        key: SettingsKey,
        value: T,
    ) {
        dataStore.edit {
            when (val preferenceKey = preferenceKeyFromSettingsKey(key)) {
                is PreferenceKey.IntKey -> it[preferenceKey.preferenceKey] = value as Int
                is PreferenceKey.StringKey -> it[preferenceKey.preferenceKey] = value as String
                is PreferenceKey.BooleanKey -> it[preferenceKey.preferenceKey] = value as Boolean
                is PreferenceKey.FloatKey -> it[preferenceKey.preferenceKey] = value as Float
                is PreferenceKey.LongKey -> it[preferenceKey.preferenceKey] = value as Long
            }
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    suspend fun remove(key: SettingsKey) {
        dataStore.edit { it.remove(preferenceKeyFromSettingsKey(key).preferenceKey) }
    }

    suspend fun contains(key: SettingsKey): Boolean {
        return dataStore.data.map { it.contains(preferenceKeyFromSettingsKey(key).preferenceKey) }.firstOrNull() ?: false
    }
}
