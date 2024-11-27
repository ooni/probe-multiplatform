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
    open val indentation: Int = 0,
)

data class SettingsItem(
    override val icon: DrawableResource? = null,
    override val title: StringResource,
    override val type: PreferenceItemType,
    override val key: SettingsKey,
    override val supportingContent: @Composable (() -> Unit)? = null,
    override val trailingContent: @Composable (() -> Unit)? = null,
    override val enabled: Boolean = true,
    override val indentation: Int = 0,
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
    override val indentation: Int = 0,
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
        fun fromValue(value: String?) = value?.let { entries.firstOrNull { it.value == value } }
    }
}
