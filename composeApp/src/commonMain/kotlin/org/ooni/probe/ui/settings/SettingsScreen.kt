package org.ooni.probe.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.CategoryCode_ALDR_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_ALDR_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_ANON_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_ANON_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_COMM_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_COMM_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_COMT_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_COMT_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_CTRL_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_CTRL_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_CULTR_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_CULTR_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_DATE_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_DATE_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_ECON_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_ECON_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_ENV_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_ENV_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_FILE_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_FILE_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_GAME_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_GAME_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_GMB_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_GMB_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_GOVT_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_GOVT_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_GRP_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_GRP_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_HACK_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_HACK_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_HATE_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_HATE_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_HOST_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_HOST_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_HUMR_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_HUMR_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_IGO_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_IGO_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_LGBT_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_LGBT_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_MILX_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_MILX_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_MMED_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_MMED_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_NEWS_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_NEWS_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_POLR_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_POLR_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_PORN_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_PORN_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_PROV_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_PROV_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_PUBH_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_PUBH_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_REL_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_REL_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_SRCH_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_SRCH_Name
import ooniprobe.composeapp.generated.resources.CategoryCode_XED_Description
import ooniprobe.composeapp.generated.resources.CategoryCode_XED_Name
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
import ooniprobe.composeapp.generated.resources.category_aldr
import ooniprobe.composeapp.generated.resources.category_anon
import ooniprobe.composeapp.generated.resources.category_comm
import ooniprobe.composeapp.generated.resources.category_comt
import ooniprobe.composeapp.generated.resources.category_ctrl
import ooniprobe.composeapp.generated.resources.category_cultr
import ooniprobe.composeapp.generated.resources.category_date
import ooniprobe.composeapp.generated.resources.category_econ
import ooniprobe.composeapp.generated.resources.category_env
import ooniprobe.composeapp.generated.resources.category_file
import ooniprobe.composeapp.generated.resources.category_game
import ooniprobe.composeapp.generated.resources.category_gmb
import ooniprobe.composeapp.generated.resources.category_govt
import ooniprobe.composeapp.generated.resources.category_grp
import ooniprobe.composeapp.generated.resources.category_hack
import ooniprobe.composeapp.generated.resources.category_hate
import ooniprobe.composeapp.generated.resources.category_host
import ooniprobe.composeapp.generated.resources.category_humr
import ooniprobe.composeapp.generated.resources.category_igo
import ooniprobe.composeapp.generated.resources.category_lgbt
import ooniprobe.composeapp.generated.resources.category_milx
import ooniprobe.composeapp.generated.resources.category_mmed
import ooniprobe.composeapp.generated.resources.category_news
import ooniprobe.composeapp.generated.resources.category_polr
import ooniprobe.composeapp.generated.resources.category_porn
import ooniprobe.composeapp.generated.resources.category_prov
import ooniprobe.composeapp.generated.resources.category_pubh
import ooniprobe.composeapp.generated.resources.category_rel
import ooniprobe.composeapp.generated.resources.category_srch
import ooniprobe.composeapp.generated.resources.category_xed
import ooniprobe.composeapp.generated.resources.ic_settings
import ooniprobe.composeapp.generated.resources.notifications
import ooniprobe.composeapp.generated.resources.outline_info
import ooniprobe.composeapp.generated.resources.privacy
import ooniprobe.composeapp.generated.resources.proxy
import ooniprobe.composeapp.generated.resources.send_email
import ooniprobe.composeapp.generated.resources.settings
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.repositories.PreferenceCategoryKey
import org.ooni.probe.data.repositories.SettingsKey
import org.ooni.probe.ui.settings.category.SettingsDescription

@Composable
fun SettingsScreen(onNavigateToSettingsCategory: (SettingsViewModel.Event) -> Unit) {
    Column {
        TopAppBar(
            title = {
                Text(stringResource(Res.string.settings))
            },
        )

        SettingsCategoryItem.getSettingsItems().forEach { item ->
            SettingsItemView(
                icon = item.icon,
                title = item.title,
                modifier =
                    Modifier.clickable {
                        onNavigateToSettingsCategory(
                            item.routeToSettingsCategory(),
                        )
                    },
            )
        }
    }
}

@Composable
fun SettingsItemView(
    icon: DrawableResource?,
    title: StringResource,
    modifier: Modifier,
) {
    ListItem(
        leadingContent = {
            icon?.let {
                Image(
                    modifier = Modifier.height(24.dp).width(24.dp),
                    painter = painterResource(it),
                    contentDescription = stringResource(title),
                )
            }
        },
        headlineContent = { Text(stringResource(title)) },
        modifier = modifier,
    )
}

open class PreferenceItem(
    open val title: StringResource,
    open val icon: DrawableResource? = null,
    open val type: PreferenceItemType,
    open val key: SettingsKey,
    open val supportingContent:
        @Composable()
        (() -> Unit)? = null,
)

data class SettingsItem(
    override val icon: DrawableResource? = null,
    override val title: StringResource,
    override val type: PreferenceItemType,
    override val key: SettingsKey,
    val children: List<SettingsItem>? = emptyList(),
    override val supportingContent:
        @Composable()
        (() -> Unit)? = null,
) : PreferenceItem(title = title, icon = icon, supportingContent = supportingContent, type = type, key = key)

data class SettingsCategoryItem(
    override val icon: DrawableResource? = null,
    override val title: StringResource,
    val route: PreferenceCategoryKey,
    val settings: List<PreferenceItem>? = emptyList(),
    override val supportingContent:
        @Composable (() -> Unit)? = null,
    val footerContent:
        @Composable (() -> Unit)? = null,
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
                    Text(stringResource(Res.string.Settings_Websites_Categories_Description))
                },
                settings =
                    listOf(
                        SettingsItem(
                            icon = Res.drawable.category_anon,
                            title = Res.string.CategoryCode_ANON_Name,
                            supportingContent = {
                                Text(stringResource(Res.string.CategoryCode_ANON_Description))
                            },
                            key = SettingsKey.ANON,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_comt,
                            title = Res.string.CategoryCode_COMT_Name,
                            supportingContent = {
                                Text(stringResource(Res.string.CategoryCode_COMT_Description))
                            },
                            key = SettingsKey.COMT,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_ctrl,
                            title = Res.string.CategoryCode_CTRL_Name,
                            supportingContent = {
                                Text(stringResource(Res.string.CategoryCode_CTRL_Description))
                            },
                            key = SettingsKey.CTRL,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_cultr,
                            title = Res.string.CategoryCode_CULTR_Name,
                            supportingContent = {
                                Text(stringResource(Res.string.CategoryCode_CULTR_Description))
                            },
                            key = SettingsKey.CULTR,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_aldr,
                            title = Res.string.CategoryCode_ALDR_Name,
                            supportingContent = {
                                Text(stringResource(Res.string.CategoryCode_ALDR_Description))
                            },
                            key = SettingsKey.ALDR,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_comm,
                            title = Res.string.CategoryCode_COMM_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_COMM_Description)) },
                            key = SettingsKey.COMM,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_econ,
                            title = Res.string.CategoryCode_ECON_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_ECON_Description)) },
                            key = SettingsKey.ECON,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_env,
                            title = Res.string.CategoryCode_ENV_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_ENV_Description)) },
                            key = SettingsKey.ENV,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_file,
                            title = Res.string.CategoryCode_FILE_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_FILE_Description)) },
                            key = SettingsKey.FILE,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_gmb,
                            title = Res.string.CategoryCode_GMB_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_GMB_Description)) },
                            key = SettingsKey.GMB,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_game,
                            title = Res.string.CategoryCode_GAME_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_GAME_Description)) },
                            key = SettingsKey.GAME,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_govt,
                            title = Res.string.CategoryCode_GOVT_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_GOVT_Description)) },
                            key = SettingsKey.GOVT,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_hack,
                            title = Res.string.CategoryCode_HACK_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_HACK_Description)) },
                            key = SettingsKey.HACK,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_hate,
                            title = Res.string.CategoryCode_HATE_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_HATE_Description)) },
                            key = SettingsKey.HATE,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_host,
                            title = Res.string.CategoryCode_HOST_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_HOST_Description)) },
                            key = SettingsKey.HOST,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_humr,
                            title = Res.string.CategoryCode_HUMR_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_HUMR_Description)) },
                            key = SettingsKey.HUMR,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_igo,
                            title = Res.string.CategoryCode_IGO_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_IGO_Description)) },
                            key = SettingsKey.IGO,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_lgbt,
                            title = Res.string.CategoryCode_LGBT_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_LGBT_Description)) },
                            key = SettingsKey.LGBT,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_mmed,
                            title = Res.string.CategoryCode_MMED_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_MMED_Description)) },
                            key = SettingsKey.MMED,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_news,
                            title = Res.string.CategoryCode_NEWS_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_NEWS_Description)) },
                            key = SettingsKey.NEWS,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_date,
                            title = Res.string.CategoryCode_DATE_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_DATE_Description)) },
                            key = SettingsKey.DATE,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_polr,
                            title = Res.string.CategoryCode_POLR_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_POLR_Description)) },
                            key = SettingsKey.POLR,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_porn,
                            title = Res.string.CategoryCode_PORN_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_PORN_Description)) },
                            key = SettingsKey.PORN,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_prov,
                            title = Res.string.CategoryCode_PROV_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_PROV_Description)) },
                            key = SettingsKey.PROV,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_pubh,
                            title = Res.string.CategoryCode_PUBH_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_PUBH_Description)) },
                            key = SettingsKey.PUBH,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_rel,
                            title = Res.string.CategoryCode_REL_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_REL_Description)) },
                            key = SettingsKey.REL,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_srch,
                            title = Res.string.CategoryCode_SRCH_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_SRCH_Description)) },
                            key = SettingsKey.SRCH,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_xed,
                            title = Res.string.CategoryCode_XED_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_XED_Description)) },
                            key = SettingsKey.XED,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_grp,
                            title = Res.string.CategoryCode_GRP_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_GRP_Description)) },
                            key = SettingsKey.GRP,
                            type = PreferenceItemType.SWITCH,
                        ),
                        SettingsItem(
                            icon = Res.drawable.category_milx,
                            title = Res.string.CategoryCode_MILX_Name,
                            supportingContent = { Text(stringResource(Res.string.CategoryCode_MILX_Description)) },
                            key = SettingsKey.MILX,
                            type = PreferenceItemType.SWITCH,
                        ),
                    ),
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
                            SettingsItem(
                                title = Res.string.Settings_Advanced_LanguageSettings_Title,
                                key = SettingsKey.LANGUAGE_SETTING,
                                type = PreferenceItemType.SELECT,
                            ),
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
