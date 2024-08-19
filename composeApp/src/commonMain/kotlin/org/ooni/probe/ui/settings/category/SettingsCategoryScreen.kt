package org.ooni.probe.ui.settings.category

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Title
import ooniprobe.composeapp.generated.resources.back
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.PreferenceCategoryKey
import org.ooni.probe.data.models.PreferenceItemType
import org.ooni.probe.data.models.SettingsCategoryItem
import org.ooni.probe.data.models.SettingsKey

@Composable
fun SettingsCategoryScreen(
    state: SettingsCategoryViewModel.State,
    onEvent: (SettingsCategoryViewModel.Event) -> Unit,
) {
    Column {
        TopAppBar(
            title = {
                Text(stringResource(state.category.title))
            },
            navigationIcon = {
                IconButton(onClick = { onEvent(SettingsCategoryViewModel.Event.BackClicked) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                    )
                }
            },
        )
        Box(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 48.dp),
        ) {
            Column {
                state.category.settings?.forEach { preferenceItem ->
                    when (preferenceItem.type) {
                        PreferenceItemType.SWITCH ->
                            SwitchSettingsView(
                                leadingContent =
                                    preferenceItem.icon?.let {
                                        {
                                            Image(
                                                modifier = Modifier.height(24.dp).width(24.dp),
                                                painter = painterResource(it),
                                                contentDescription = stringResource(preferenceItem.title),
                                            )
                                        }
                                    },
                                title = preferenceItem.title,
                                key = preferenceItem.key,
                                checked =
                                    state.preference?.let { it[preferenceItem.key] as? Boolean }
                                        ?: false,
                                supportingContent = preferenceItem.supportingContent,
                                onCheckedChange = { key, value ->
                                    onEvent(
                                        SettingsCategoryViewModel.Event.CheckedChangeClick(
                                            key,
                                            value,
                                        ),
                                    )
                                },
                            )

                        PreferenceItemType.TEXT ->
                            RouteSettingsView(
                                title = preferenceItem.title,
                                supportingContent = preferenceItem.supportingContent,
                            )

                        PreferenceItemType.BUTTON ->
                            RouteSettingsView(
                                title = preferenceItem.title,
                                supportingContent = preferenceItem.supportingContent,
                                trailingContent = {
                                    Button(
                                        onClick = {},
                                    ) {
                                        Text(stringResource(Res.string.Settings_Title))
                                    }
                                },
                            )

                        PreferenceItemType.ROUTE ->
                            RouteSettingsView(
                                title = preferenceItem.title,
                                supportingContent = preferenceItem.supportingContent,
                                modifier =
                                    Modifier.clickable {
                                        if (preferenceItem is SettingsCategoryItem) {
                                            onEvent(
                                                SettingsCategoryViewModel.Event.SettingsCategoryClick(
                                                    PreferenceCategoryKey.valueOf(preferenceItem.route.name),
                                                ),
                                            )
                                        }
                                    },
                            )

                        PreferenceItemType.SELECT ->
                            RouteSettingsView(
                                title = preferenceItem.title,
                                supportingContent = preferenceItem.supportingContent,
                            )
                    }
                }
                state.category.footerContent?.invoke()
            }
        }
    }
}

@Composable
fun SwitchSettingsView(
    title: StringResource,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    key: SettingsKey,
    checked: Boolean,
    onCheckedChange: (SettingsKey, Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = { newValue -> onCheckedChange(key, newValue) },
                modifier = Modifier.scale(0.7f),
            )
        },
    )
}

@Composable
fun RouteSettingsView(
    title: StringResource,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        modifier = modifier,
    )
}

@Composable
fun SettingsDescription(description: StringResource) {
    Text(
        stringResource(description),
        modifier = Modifier.padding(horizontal = 16.dp),
        fontSize = 12.sp,
    )
}
