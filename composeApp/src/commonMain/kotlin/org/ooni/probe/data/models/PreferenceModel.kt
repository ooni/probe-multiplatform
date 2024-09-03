package org.ooni.probe.data.models

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import ooniprobe.composeapp.generated.resources.Modal_EnableNotifications_Paragraph
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_About_Label
import ooniprobe.composeapp.generated.resources.Settings_Advanced_DebugLogs
import ooniprobe.composeapp.generated.resources.Settings_Advanced_Label
import ooniprobe.composeapp.generated.resources.Settings_Advanced_LanguageSettings_Title
import ooniprobe.composeapp.generated.resources.Settings_Advanced_RecentLogs
import ooniprobe.composeapp.generated.resources.Settings_AutomatedTesting_RunAutomatically
import ooniprobe.composeapp.generated.resources.Settings_AutomatedTesting_RunAutomatically_ChargingOnly
import ooniprobe.composeapp.generated.resources.Settings_AutomatedTesting_RunAutomatically_Footer
import ooniprobe.composeapp.generated.resources.Settings_AutomatedTesting_RunAutomatically_WiFiOnly
import ooniprobe.composeapp.generated.resources.Settings_Notifications_Enabled
import ooniprobe.composeapp.generated.resources.Settings_Notifications_Label
import ooniprobe.composeapp.generated.resources.Settings_Privacy_Label
import ooniprobe.composeapp.generated.resources.Settings_Privacy_SendCrashReports
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Label
import ooniprobe.composeapp.generated.resources.Settings_SendEmail_Label
import ooniprobe.composeapp.generated.resources.Settings_Sharing_UploadResults
import ooniprobe.composeapp.generated.resources.Settings_Storage_Label
import ooniprobe.composeapp.generated.resources.Settings_TestOptions_Label
import ooniprobe.composeapp.generated.resources.Settings_WarmVPNInUse_Label
import ooniprobe.composeapp.generated.resources.Settings_Websites_Categories_Description
import ooniprobe.composeapp.generated.resources.Settings_Websites_Categories_Label
import ooniprobe.composeapp.generated.resources.Settings_Websites_MaxRuntime
import ooniprobe.composeapp.generated.resources.Settings_Websites_MaxRuntimeEnabled
import ooniprobe.composeapp.generated.resources.advanced
import ooniprobe.composeapp.generated.resources.ic_settings
import ooniprobe.composeapp.generated.resources.notifications
import ooniprobe.composeapp.generated.resources.outline_info
import ooniprobe.composeapp.generated.resources.privacy
import ooniprobe.composeapp.generated.resources.proxy
import ooniprobe.composeapp.generated.resources.send_email
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.ui.settings.SettingsViewModel
import org.ooni.probe.ui.settings.category.SettingsDescription

open class PreferenceItem(
    open val title: StringResource,
    open val icon: DrawableResource? = null,
    open val type: PreferenceItemType,
    open val key: SettingsKey,
    open val supportingContent: @Composable (() -> Unit)? = null,
)

data class SettingsItem(
    override val icon: DrawableResource? = null,
    override val title: StringResource,
    override val type: PreferenceItemType,
    override val key: SettingsKey,
    val children: List<SettingsItem>? = emptyList(),
    override val supportingContent: @Composable (() -> Unit)? = null,
) : PreferenceItem(
        title = title,
        icon = icon,
        supportingContent = supportingContent,
        type = type,
        key = key,
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
    ) {
    fun routeToSettingsCategory() = SettingsViewModel.Event.SettingsCategoryClick(route)

    companion object {
        private val seeRecentLogsCategory =
            SettingsCategoryItem(
                title = Res.string.Settings_Advanced_RecentLogs,
                route = PreferenceCategoryKey.SEE_RECENT_LOGS,
            )
        private val webCategory =
            SettingsCategoryItem(
                title = Res.string.Settings_Websites_Categories_Label,
                route = PreferenceCategoryKey.WEBSITES_CATEGORIES,
                supportingContent = {
                    // TODO(norbel): add enabled categories
                    Text(stringResource(Res.string.Settings_Websites_Categories_Description))
                },
                settings = WebConnectivityCategory.entries
                    .mapNotNull { cat ->
                        SettingsItem(
                            icon = cat.icon,
                            title = cat.title,
                            supportingContent = { Text(stringResource(cat.description)) },
                            key = cat.settingsKey ?: return@mapNotNull null,
                            type = PreferenceItemType.SWITCH,
                        )
                    },
            )

        fun getSettingsItems() =
            listOf(
                SettingsCategoryItem(
                    icon = Res.drawable.notifications,
                    title = Res.string.Settings_Notifications_Label,
                    route = PreferenceCategoryKey.NOTIFICATIONS,
                    settings =
                        listOf(
                            SettingsItem(
                                title = Res.string.Settings_Notifications_Enabled,
                                key = SettingsKey.NOTIFICATIONS_ENABLED,
                                type = PreferenceItemType.SWITCH,
                            ),
                        ),
                    footerContent = {
                        SettingsDescription(
                            Res.string.Modal_EnableNotifications_Paragraph,
                        )
                    },
                ),
                SettingsCategoryItem(
                    icon = Res.drawable.ic_settings,
                    title = Res.string.Settings_TestOptions_Label,
                    route = PreferenceCategoryKey.TEST_OPTIONS,
                    settings =
                        listOf(
                            SettingsItem(
                                title = Res.string.Settings_AutomatedTesting_RunAutomatically,
                                key = SettingsKey.AUTOMATED_TESTING_ENABLED,
                                type = PreferenceItemType.SWITCH,
                            ),
                            SettingsItem(
                                title = Res.string.Settings_AutomatedTesting_RunAutomatically_WiFiOnly,
                                key = SettingsKey.AUTOMATED_TESTING_WIFIONLY,
                                type = PreferenceItemType.SWITCH,
                            ),
                            SettingsItem(
                                title = Res.string.Settings_AutomatedTesting_RunAutomatically_ChargingOnly,
                                key = SettingsKey.AUTOMATED_TESTING_CHARGING,
                                type = PreferenceItemType.SWITCH,
                            ),
                            webCategory,
                            SettingsItem(
                                title = Res.string.Settings_Websites_MaxRuntimeEnabled,
                                key = SettingsKey.MAX_RUNTIME_ENABLED,
                                type = PreferenceItemType.SWITCH,
                            ),
                            SettingsItem(
                                title = Res.string.Settings_Websites_MaxRuntime,
                                key = SettingsKey.MAX_RUNTIME,
                                type = PreferenceItemType.TEXT,
                            ),
                        ),
                    footerContent = {
                        SettingsDescription(
                            Res.string.Settings_AutomatedTesting_RunAutomatically_Footer,
                        )
                    },
                ),
                SettingsCategoryItem(
                    icon = Res.drawable.privacy,
                    title = Res.string.Settings_Privacy_Label,
                    route = PreferenceCategoryKey.PRIVACY,
                    settings =
                        listOf(
                            SettingsItem(
                                title = Res.string.Settings_Sharing_UploadResults,
                                key = SettingsKey.UPLOAD_RESULTS,
                                type = PreferenceItemType.SWITCH,
                            ),
                            SettingsItem(
                                title = Res.string.Settings_Privacy_SendCrashReports,
                                key = SettingsKey.SEND_CRASH,
                                type = PreferenceItemType.SWITCH,
                            ),
                        ),
                ),
                SettingsCategoryItem(
                    icon = Res.drawable.proxy,
                    title = Res.string.Settings_Proxy_Label,
                    route = PreferenceCategoryKey.PROXY,
                ),
                SettingsCategoryItem(
                    icon = Res.drawable.advanced,
                    title = Res.string.Settings_Advanced_Label,
                    route = PreferenceCategoryKey.ADVANCED,
                    settings =
                        listOf(
                            // TODO(aanorbel) : Add language settings when in app language switcher can be implemented
                            seeRecentLogsCategory,
                            SettingsItem(
                                title = Res.string.Settings_Advanced_DebugLogs,
                                key = SettingsKey.DEBUG_LOGS,
                                type = PreferenceItemType.SWITCH,
                            ),
                            SettingsItem(
                                title = Res.string.Settings_Storage_Label,
                                key = SettingsKey.STORAGE_SIZE,
                                type = PreferenceItemType.BUTTON,
                            ),
                            SettingsItem(
                                title = Res.string.Settings_WarmVPNInUse_Label,
                                key = SettingsKey.WARN_VPN_IN_USE,
                                type = PreferenceItemType.SWITCH,
                            ),
                        ),
                ),
                SettingsCategoryItem(
                    icon = Res.drawable.send_email,
                    title = Res.string.Settings_SendEmail_Label,
                    route = PreferenceCategoryKey.SEND_EMAIL,
                ),
                SettingsCategoryItem(
                    icon = Res.drawable.outline_info,
                    title = Res.string.Settings_About_Label,
                    route = PreferenceCategoryKey.ABOUT_OONI,
                ),
            )

        fun getSettingsItem(route: PreferenceCategoryKey) =
            (getSettingsItems() + listOf(webCategory, seeRecentLogsCategory)).first {
                it.route == route
            }
    }
}

enum class PreferenceItemType {
    SWITCH,
    TEXT,
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
