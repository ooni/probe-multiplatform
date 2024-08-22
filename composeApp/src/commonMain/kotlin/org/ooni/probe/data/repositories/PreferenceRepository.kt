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
import org.ooni.probe.data.models.ProxyProtocol
import org.ooni.probe.data.models.SettingsKey

const val IP_ADDRESS = (
    "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]" +
        "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]" +
        "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}" +
        "|[1-9][0-9]|[0-9]))"
)

/**
 * Regex for an IPv6 Address.
 *
 * Note that this value is adapted from the following StackOverflow answer:
 * https://stackoverflow.com/a/17871737/1478764
 */
const val IPV6_ADDRESS =
    (
        "(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}" +
            "|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|" +
            "([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|" +
            "[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4})" +
            "{0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.)" +
            "{3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}" +
            "[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))"
    )

const val DOMAIN_NAME = ("((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}")

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
            keys.map { key ->
                key to it[
                    preferenceKeyFromSettingsKey(
                        key, prefix, autoRun,
                    ).preferenceKey,
                ]
            }.toMap()
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

    suspend fun getProxyString(): String {
        val proxyHost =
            getValueByKey(SettingsKey.PROXY_HOSTNAME).firstOrNull() as String? ?: return ""
        val proxyPort = getValueByKey(SettingsKey.PROXY_PORT).firstOrNull() as Int? ?: return ""

        when (
            val proxyProtocol =
                getValueByKey(SettingsKey.PROXY_PROTOCOL).firstOrNull() as String?
        ) {
            ProxyProtocol.NONE.protocol -> return ""
            ProxyProtocol.PSIPHON.protocol -> return "psiphon://"
            ProxyProtocol.SOCKS5.protocol, ProxyProtocol.HTTP.protocol, ProxyProtocol.HTTPS.protocol -> {
                val formattedHost = if (isIPv6(proxyHost)) {
                    "[$proxyHost]"
                } else {
                    proxyHost
                }
                return "$proxyProtocol://$formattedHost:$proxyPort/"
            }

            else -> return ""
        }
    }

    private fun isIPv6(hostname: String): Boolean {
        return IPV6_ADDRESS.toRegex().matches(hostname)
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    suspend fun remove(key: SettingsKey) {
        dataStore.edit { it.remove(preferenceKeyFromSettingsKey(key).preferenceKey) }
    }

    suspend fun contains(key: SettingsKey): Boolean {
        return dataStore.data.map { it.contains(preferenceKeyFromSettingsKey(key).preferenceKey) }
            .firstOrNull() ?: false
    }
}
