package org.ooni.probe.data.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

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
    fun getPreferenceKey(
        name: String,
        prefix: String? = null,
        autoRun: Boolean = false,
    ): String {
        return "${prefix?.let { "${it}_" } ?: ""}$name${if (autoRun) "_autorun" else ""}"
    }

    fun preferenceKeyFromSettingsKey(
        key: SettingsKey,
        prefix: String? = null,
        autoRun: Boolean = false,
    ): Preferences.Key<*> {
        val preferenceKey = getPreferenceKey(name = key.value, prefix = prefix, autoRun = autoRun)
        return when (key) {
            SettingsKey.MAX_RUNTIME -> intPreferencesKey(preferenceKey)
            SettingsKey.PROXY_PORT -> intPreferencesKey(preferenceKey)
            SettingsKey.PROXY_HOSTNAME -> stringPreferencesKey(preferenceKey)
            SettingsKey.PROXY_PROTOCOL -> stringPreferencesKey(preferenceKey)
            SettingsKey.LANGUAGE_SETTING -> stringPreferencesKey(preferenceKey)
            else -> booleanPreferencesKey(preferenceKey)
        }
    }

    fun allSettings(
        keys: List<SettingsKey>,
        prefix: String? = null,
        autoRun: Boolean = false,
    ): Flow<Map<SettingsKey, Any?>> =
        dataStore.data.map {
            keys.map { key -> key to it[preferenceKeyFromSettingsKey(key, prefix, autoRun)] }.toMap()
        }

    fun <T> getValueByKey(key: Preferences.Key<T>): Flow<T?> {
        return dataStore.data.map { it[key] }
    }

    suspend fun <T> setValueByKey(
        key: Preferences.Key<T>,
        value: T,
    ) {
        dataStore.edit { it[key] = value }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    suspend fun remove(key: Preferences.Key<*>) {
        dataStore.edit { it.remove(key) }
    }

    suspend fun contains(key: Preferences.Key<*>): Boolean {
        return dataStore.data.map { it.contains(key) }.firstOrNull() ?: false
    }
}

enum class PreferenceCategoryKey(val value: String) {
    NOTIFICATIONS("notifications"),
    TEST_OPTIONS("test_options"),
    PRIVACY("privacy"),
    PROXY("proxy"),
    ADVANCED("advanced"),
    SEND_EMAIL("send_email"),
    ABOUT_OONI("about_ooni"),

    WEBSITES_CATEGORIES("websites_categories"),
    SEE_RECENT_LOGS("see_recent_logs"),
}

enum class SettingsKey(val value: String) {
    // Notifications
    NOTIFICATIONS_ENABLED("notifications_enabled"),

    // Test Options
    AUTOMATED_TESTING_ENABLED("automated_testing_enabled"),
    AUTOMATED_TESTING_WIFIONLY("automated_testing_wifionly"),
    AUTOMATED_TESTING_CHARGING("automated_testing_charging"),
    MAX_RUNTIME_ENABLED("max_runtime_enabled"),
    MAX_RUNTIME("max_runtime"),

    // Website categories
    SRCH("SRCH"),
    PORN("PORN"),
    COMM("COMM"),
    COMT("COMT"),
    MMED("MMED"),
    HATE("HATE"),
    POLR("POLR"),
    PUBH("PUBH"),
    GAME("GAME"),
    PROV("PROV"),
    HACK("HACK"),
    MILX("MILX"),
    DATE("DATE"),
    ANON("ANON"),
    ALDR("ALDR"),
    GMB("GMB"),
    XED("XED"),
    REL("REL"),
    GRP("GRP"),
    GOVT("GOVT"),
    ECON("ECON"),
    LGBT("LGBT"),
    FILE("FILE"),
    HOST("HOST"),
    HUMR("HUMR"),
    NEWS("NEWS"),
    ENV("ENV"),
    CULTR("CULTR"),
    CTRL("CTRL"),
    IGO("IGO"),

    // Privacy
    UPLOAD_RESULTS("upload_results"),
    SEND_CRASH("send_crash"),

    // Proxy
    PROXY_HOSTNAME("proxy_hostname"),
    PROXY_PORT("proxy_port"),

    // Advanced
    THEME_ENABLED("theme_enabled"),
    LANGUAGE_SETTING("language_setting"),
    DEBUG_LOGS("debugLogs"),
    WARN_VPN_IN_USE("warn_vpn_in_use"),
    STORAGE_SIZE("storage_size"), // purely decorative

    // MISC
    DELETE_UPLOADED_JSONS("deleteUploadedJsons"),
    IS_NOTIFICATION_DIALOG("isNotificationDialog"),
    FIRST_RUN("first_run"),

    // Run Tests
    TEST_SIGNAL("test_signal"),
    RUN_HTTP_INVALID_REQUEST_LINE("run_http_invalid_request_line"),
    TEST_FACEBOOK_MESSENGER("test_facebook_messenger"),
    RUN_DASH("run_dash"),
    WEB_CONNECTIVITY("web_connectivity"),
    RUN_NDT("run_ndt"),
    TEST_PSIPHON("test_psiphon"),
    TEST_TOR("test_tor"),
    PROXY_PROTOCOL("proxy_protocol"),
    TEST_TELEGRAM("test_telegram"),
    RUN_HTTP_HEADER_FIELD_MANIPULATION("run_http_header_field_manipulation"),
    EXPERIMENTAL("experimental"),
    TEST_WHATSAPP("test_whatsapp"),

    ROUTE("route"),
}
