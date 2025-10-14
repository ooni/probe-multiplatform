package org.ooni.probe.data.repositories

import androidx.annotation.VisibleForTesting
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.SettingsKey

sealed class PreferenceKey<T>(
    val preferenceKey: Preferences.Key<T>,
) {
    class IntKey(
        preferenceKey: Preferences.Key<Int>,
    ) : PreferenceKey<Int>(preferenceKey)

    class StringKey(
        preferenceKey: Preferences.Key<String>,
    ) : PreferenceKey<String>(preferenceKey)

    class StringSetKey(
        preferenceKey: Preferences.Key<Set<String>>,
    ) : PreferenceKey<Set<String>>(preferenceKey)

    class BooleanKey(
        preferenceKey: Preferences.Key<Boolean>,
    ) : PreferenceKey<Boolean>(preferenceKey)

    class FloatKey(
        preferenceKey: Preferences.Key<Float>,
    ) : PreferenceKey<Float>(preferenceKey)

    class LongKey(
        preferenceKey: Preferences.Key<Long>,
    ) : PreferenceKey<Long>(preferenceKey)
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
    ): String = "${prefix?.let { "${it}_" } ?: ""}${if (autoRun) "autorun_" else ""}$name"

    private fun preferenceKeyFromSettingsKey(
        key: SettingsKey,
        prefix: String? = null,
        autoRun: Boolean = false,
    ): PreferenceKey<*> {
        val preferenceKey = getPreferenceKey(name = key.value, prefix = prefix, autoRun = autoRun)
        return when (key) {
            SettingsKey.MAX_RUNTIME,
            SettingsKey.LEGACY_PROXY_PORT,
            SettingsKey.DELETE_OLD_RESULTS_THRESHOLD,
            -> PreferenceKey.IntKey(intPreferencesKey(preferenceKey))

            SettingsKey.LAST_ARTICLES_REFRESH,
            -> PreferenceKey.LongKey(longPreferencesKey(preferenceKey))

            SettingsKey.LEGACY_PROXY_HOSTNAME,
            SettingsKey.LEGACY_PROXY_PROTOCOL,
            SettingsKey.PROXY_SELECTED,
            SettingsKey.LANGUAGE_SETTING,
            SettingsKey.LAST_RUN_DISMISSED,
            -> PreferenceKey.StringKey(stringPreferencesKey(preferenceKey))

            SettingsKey.CHOSEN_WEBSITES,
            SettingsKey.PROXIES_CUSTOM,
            SettingsKey.DESCRIPTOR_SECTIONS_COLLAPSED,
            -> PreferenceKey.StringSetKey(stringSetPreferencesKey(preferenceKey))

            else -> PreferenceKey.BooleanKey(booleanPreferencesKey(preferenceKey))
        }
    }

    fun allSettings(
        keys: List<SettingsKey>,
        prefix: String? = null,
        autoRun: Boolean = false,
    ): Flow<Map<SettingsKey, Any?>> =
        dataStore.data
            .map {
                keys.associateWith { key ->
                    it[preferenceKeyFromSettingsKey(key, prefix, autoRun).preferenceKey]
                }
            }.distinctUntilChanged()

    fun getValueByKey(key: SettingsKey): Flow<Any?> =
        dataStore.data
            .map {
                when (val preferenceKey = preferenceKeyFromSettingsKey(key)) {
                    is PreferenceKey.IntKey -> it[preferenceKey.preferenceKey]
                    is PreferenceKey.StringKey -> it[preferenceKey.preferenceKey]
                    is PreferenceKey.StringSetKey -> it[preferenceKey.preferenceKey]
                    is PreferenceKey.BooleanKey -> it[preferenceKey.preferenceKey]
                    is PreferenceKey.FloatKey -> it[preferenceKey.preferenceKey]
                    is PreferenceKey.LongKey -> it[preferenceKey.preferenceKey]
                }
            }.distinctUntilChanged()

    suspend fun setValueByKey(
        key: SettingsKey,
        value: Any?,
    ) {
        setValuesByKey(listOf(key to value))
    }

    suspend fun setValuesByKey(pairs: List<Pair<SettingsKey, Any?>>) {
        dataStore.edit {
            pairs.forEach { (key, value) ->
                @Suppress("UNCHECKED_CAST")
                when (val preferenceKey = preferenceKeyFromSettingsKey(key)) {
                    is PreferenceKey.IntKey ->
                        it[preferenceKey.preferenceKey] = value as Int

                    is PreferenceKey.StringKey ->
                        it[preferenceKey.preferenceKey] = value as String

                    is PreferenceKey.StringSetKey ->
                        it[preferenceKey.preferenceKey] = value as Set<String>

                    is PreferenceKey.BooleanKey ->
                        it[preferenceKey.preferenceKey] = value as Boolean

                    is PreferenceKey.FloatKey ->
                        it[preferenceKey.preferenceKey] = value as Float

                    is PreferenceKey.LongKey ->
                        it[preferenceKey.preferenceKey] = value as Long
                }
            }
        }
    }

    fun isNetTestEnabled(
        descriptor: Descriptor,
        netTest: NetTest,
        isAutoRun: Boolean,
    ): Flow<Boolean> =
        dataStore.data
            .map {
                it[booleanPreferencesKey(getNetTestKey(descriptor, netTest, isAutoRun))]
                    ?: netTest.defaultPreferenceValue(isAutoRun)
            }.distinctUntilChanged()

    fun areNetTestsEnabled(
        list: List<Pair<Descriptor, NetTest>>,
        isAutoRun: Boolean,
    ): Flow<Map<Pair<Descriptor, NetTest>, Boolean>> =
        dataStore.data
            .map {
                list.associate { (descriptor, netTest) ->
                    Pair(descriptor, netTest) to (
                        it[booleanPreferencesKey(getNetTestKey(descriptor, netTest, isAutoRun))]
                            ?: netTest.defaultPreferenceValue(isAutoRun)
                    )
                }
            }.distinctUntilChanged()

    suspend fun setAreNetTestsEnabled(
        list: List<Pair<Descriptor, NetTest>>,
        isAutoRun: Boolean,
        isEnabled: Boolean,
    ) {
        dataStore.edit {
            list.forEach { (descriptor, netTest) ->
                it[booleanPreferencesKey(getNetTestKey(descriptor, netTest, isAutoRun))] = isEnabled
            }
        }
    }

    suspend fun setAreNetTestsEnabled(
        list: List<Pair<Descriptor, NetTest>>,
        isAutoRun: Boolean,
    ) {
        dataStore.edit {
            list.forEach { (descriptor, netTest) ->
                it[booleanPreferencesKey(getNetTestKey(descriptor, netTest, isAutoRun))] =
                    netTest.defaultPreferenceValue(isAutoRun)
            }
        }
    }

    private fun getNetTestKey(
        descriptor: Descriptor,
        netTest: NetTest,
        isAutoRun: Boolean,
    ) = getPreferenceKey(
        name = netTest.test.preferenceKey,
        prefix = (descriptor.source as? Descriptor.Source.Installed)
            ?.value
            ?.id
            ?.value,
        autoRun = isAutoRun,
    )

    suspend fun removeDescriptorPreferences(descriptor: Descriptor) {
        descriptor.netTests.forEach { netTest ->
            dataStore.edit {
                it.remove(
                    booleanPreferencesKey(getNetTestKey(descriptor, netTest, isAutoRun = true)),
                )
                it.remove(
                    booleanPreferencesKey(getNetTestKey(descriptor, netTest, isAutoRun = false)),
                )
            }
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    suspend fun remove(key: SettingsKey) {
        dataStore.edit { it.remove(preferenceKeyFromSettingsKey(key).preferenceKey) }
    }

    suspend fun contains(key: SettingsKey): Boolean =
        dataStore.data
            .map { it.contains(preferenceKeyFromSettingsKey(key).preferenceKey) }
            .firstOrNull() ?: false

    private fun NetTest.defaultPreferenceValue(isAutoRun: Boolean): Boolean =
        if (isAutoRun) test.isBackgroundRunEnabled else test.isManualRunEnabled
}
