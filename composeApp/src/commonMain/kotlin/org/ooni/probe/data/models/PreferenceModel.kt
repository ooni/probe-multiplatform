package org.ooni.probe.data.models

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

open class PreferenceItem(
    open val title: StringResource,
    open val icon: DrawableResource? = null,
    open val type: PreferenceItemType,
    open val key: SettingsKey,
    open val supportingContent: @Composable (() -> Unit)? = null,
    open val trailingContent: @Composable (() -> Unit)? = null,
    open val enabled: Boolean = true,
)

data class SettingsItem(
    override val icon: DrawableResource? = null,
    override val title: StringResource,
    override val type: PreferenceItemType,
    override val key: SettingsKey,
    override val supportingContent: @Composable (() -> Unit)? = null,
    override val trailingContent: @Composable (() -> Unit)? = null,
    override val enabled: Boolean = true,
) : PreferenceItem(
        title = title,
        icon = icon,
        supportingContent = supportingContent,
        type = type,
        key = key,
        enabled = enabled,
    )

data class SettingsCategoryItem(
    override val icon: DrawableResource? = null,
    override val title: StringResource,
    val route: PreferenceCategoryKey,
    val settings: List<PreferenceItem>? = emptyList(),
    override val supportingContent: @Composable (() -> Unit)? = null,
    val footerContent: @Composable (() -> Unit)? = null,
) : PreferenceItem(
        title = title,
        icon = icon,
        supportingContent = supportingContent,
        type = PreferenceItemType.ROUTE,
        key = SettingsKey.ROUTE,
    )

enum class PreferenceItemType {
    SWITCH,
    INT,
    BUTTON,
    SELECT,
    ROUTE,
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
    ;

    companion object {
        fun fromValue(value: String) = entries.first { it.value == value }
    }
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
