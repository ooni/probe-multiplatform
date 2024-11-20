package org.ooni.probe.domain

import androidx.compose.material3.Text
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Websites_Categories_Description
import ooniprobe.composeapp.generated.resources.Settings_Websites_Categories_Label
import ooniprobe.composeapp.generated.resources.Settings_Websites_MaxRuntime
import ooniprobe.composeapp.generated.resources.Settings_Websites_MaxRuntimeEnabled
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.data.models.PreferenceCategoryKey
import org.ooni.probe.data.models.PreferenceItem
import org.ooni.probe.data.models.PreferenceItemType
import org.ooni.probe.data.models.SettingsCategoryItem
import org.ooni.probe.data.models.SettingsItem
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.ui.shared.shortFormat
import kotlin.time.Duration.Companion.seconds

fun webConnectivityPreferences(
    enabledCategoriesCount: Int,
    maxRuntimeEnabled: Boolean,
    maxRuntime: Int?,
): List<PreferenceItem> {
    return listOf(
        SettingsCategoryItem(
            title = Res.string.Settings_Websites_Categories_Label,
            route = PreferenceCategoryKey.WEBSITES_CATEGORIES,
            supportingContent = {
                Text(
                    stringResource(
                        Res.string.Settings_Websites_Categories_Description,
                        enabledCategoriesCount,
                    ),
                )
            },
            settings = WebConnectivityCategory.entries.mapNotNull { cat ->
                SettingsItem(
                    icon = cat.icon,
                    title = cat.title,
                    supportingContent = { Text(stringResource(cat.description)) },
                    key = cat.settingsKey ?: return@mapNotNull null,
                    type = PreferenceItemType.SWITCH,
                )
            },
        ),
        SettingsItem(
            title = Res.string.Settings_Websites_MaxRuntimeEnabled,
            key = SettingsKey.MAX_RUNTIME_ENABLED,
            type = PreferenceItemType.SWITCH,
        ),
        SettingsItem(
            title = Res.string.Settings_Websites_MaxRuntime,
            key = SettingsKey.MAX_RUNTIME,
            type = PreferenceItemType.INT,
            enabled = maxRuntimeEnabled,
            supportingContent = {
                maxRuntime?.let {
                    Text(it.coerceAtLeast(0).seconds.shortFormat())
                }
            },
        ),
    )
}

fun preferenceDefaults(): List<Pair<SettingsKey, Any>> {
    return listOf(
        SettingsKey.MAX_RUNTIME_ENABLED to true,
        SettingsKey.MAX_RUNTIME to 90,
    ) + WebConnectivityCategory.entries
        .mapNotNull { it.settingsKey }
        .map { it to true }
}
