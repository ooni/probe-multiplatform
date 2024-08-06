package org.ooni.probe.ui.settings.category

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ooniprobe.composeapp.generated.resources.Modal_EnableNotifications_Paragraph
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_About_Label
import ooniprobe.composeapp.generated.resources.Settings_Advanced_Label
import ooniprobe.composeapp.generated.resources.Settings_AutomatedTesting_RunAutomatically
import ooniprobe.composeapp.generated.resources.Settings_AutomatedTesting_RunAutomatically_ChargingOnly
import ooniprobe.composeapp.generated.resources.Settings_AutomatedTesting_RunAutomatically_Footer
import ooniprobe.composeapp.generated.resources.Settings_AutomatedTesting_RunAutomatically_WiFiOnly
import ooniprobe.composeapp.generated.resources.Settings_Notifications_Enabled
import ooniprobe.composeapp.generated.resources.Settings_Notifications_Label
import ooniprobe.composeapp.generated.resources.Settings_Privacy_Label
import ooniprobe.composeapp.generated.resources.Settings_Privacy_SendCrashReports
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Label
import ooniprobe.composeapp.generated.resources.Settings_Sharing_UploadResults
import ooniprobe.composeapp.generated.resources.Settings_TestOptions_Label
import ooniprobe.composeapp.generated.resources.Settings_Websites_Categories_Description
import ooniprobe.composeapp.generated.resources.Settings_Websites_Categories_Label
import ooniprobe.composeapp.generated.resources.Settings_Websites_MaxRuntime
import ooniprobe.composeapp.generated.resources.Settings_Websites_MaxRuntimeEnabled
import ooniprobe.composeapp.generated.resources.about_ooni
import ooniprobe.composeapp.generated.resources.advanced
import ooniprobe.composeapp.generated.resources.automated_testing_charging
import ooniprobe.composeapp.generated.resources.automated_testing_enabled
import ooniprobe.composeapp.generated.resources.automated_testing_wifionly
import ooniprobe.composeapp.generated.resources.back
import ooniprobe.composeapp.generated.resources.max_runtime
import ooniprobe.composeapp.generated.resources.max_runtime_enabled
import ooniprobe.composeapp.generated.resources.notifications
import ooniprobe.composeapp.generated.resources.notifications_enabled
import ooniprobe.composeapp.generated.resources.ooni_backend_proxy
import ooniprobe.composeapp.generated.resources.privacy
import ooniprobe.composeapp.generated.resources.send_crash
import ooniprobe.composeapp.generated.resources.test_options
import ooniprobe.composeapp.generated.resources.upload_results
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsCategoryScreen(
    category: String,
    onEvent: (SettingsCategoryViewModel.Event) -> Unit,
) {
    val categories =
        mapOf(
            stringResource(Res.string.notifications) to
                SettingsCategory(
                    title = Res.string.Settings_Notifications_Label,
                    content = { NotificationsSettingsScreen(onEvent) },
                ),
            stringResource(Res.string.test_options) to
                SettingsCategory(
                    title = Res.string.Settings_TestOptions_Label,
                    content = { TestOptionsSettingsScreen(onEvent) },
                ),
            stringResource(Res.string.privacy) to
                SettingsCategory(
                    title = Res.string.Settings_Privacy_Label,
                    content = { PrivacySettingsScreen(onEvent) },
                ),
            stringResource(Res.string.advanced) to
                SettingsCategory(
                    title = Res.string.Settings_Advanced_Label,
                    content = { return@SettingsCategory },
                ),
            stringResource(Res.string.ooni_backend_proxy) to
                SettingsCategory(
                    title = Res.string.Settings_Proxy_Label,
                    content = { return@SettingsCategory },
                ),
            stringResource(Res.string.about_ooni) to
                SettingsCategory(
                    title = Res.string.Settings_About_Label,
                    content = { return@SettingsCategory },
                ),
        )

    Column {
        TopAppBar(
            title = {
                categories[category]?.let { Text(stringResource(it.title)) }
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
        categories[category]?.content?.invoke()
    }
}

data class SettingsCategory(
    val title: StringResource,
    val content: @Composable () -> Unit,
)

@Composable
fun NotificationsSettingsScreen(onEvent: (SettingsCategoryViewModel.Event) -> Unit) {
    Column {
        SwitchSettings(
            title = Res.string.Settings_Notifications_Enabled,
            key = stringResource(Res.string.notifications_enabled),
            checked = false,
            onCheckedChange = { key, value ->
                onEvent(
                    SettingsCategoryViewModel.Event.CheckedChangeClick(
                        key,
                        value,
                    ),
                )
            },
        )
        SettingsDescription(
            Res.string.Modal_EnableNotifications_Paragraph,
        )
    }
}

@Composable
fun TestOptionsSettingsScreen(onEvent: (SettingsCategoryViewModel.Event) -> Unit) {
    Column {
        SwitchSettings(
            title = Res.string.Settings_AutomatedTesting_RunAutomatically,
            key = stringResource(Res.string.automated_testing_enabled),
            checked = false,
            onCheckedChange = { key, value ->
                onEvent(
                    SettingsCategoryViewModel.Event.CheckedChangeClick(
                        key,
                        value,
                    ),
                )
            },
        )
        // TODO: Add dependency on status of automated testing above
        SwitchSettings(
            title = Res.string.Settings_AutomatedTesting_RunAutomatically_WiFiOnly,
            key = stringResource(Res.string.automated_testing_wifionly),
            checked = false,
            onCheckedChange = { key, value ->
                onEvent(
                    SettingsCategoryViewModel.Event.CheckedChangeClick(
                        key,
                        value,
                    ),
                )
            },
        )
        SwitchSettings(
            title = Res.string.Settings_AutomatedTesting_RunAutomatically_ChargingOnly,
            key = stringResource(Res.string.automated_testing_charging),
            checked = false,
            onCheckedChange = { key, value ->
                onEvent(
                    SettingsCategoryViewModel.Event.CheckedChangeClick(
                        key,
                        value,
                    ),
                )
            },
        )
        // TODO: add proper structure to navigate to the websites categories
        SwitchSettings(
            title = Res.string.Settings_Websites_Categories_Label,
            supportingContent = {
                Text(stringResource(Res.string.Settings_Websites_Categories_Description))
            },
            key = stringResource(Res.string.automated_testing_charging),
            checked = false,
            onCheckedChange = { key, value ->
                onEvent(
                    SettingsCategoryViewModel.Event.CheckedChangeClick(
                        key,
                        value,
                    ),
                )
            },
        )
        SwitchSettings(
            title = Res.string.Settings_Websites_MaxRuntimeEnabled,
            key = stringResource(Res.string.max_runtime_enabled),
            checked = false,
            onCheckedChange = { key, value ->
                onEvent(
                    SettingsCategoryViewModel.Event.CheckedChangeClick(
                        key,
                        value,
                    ),
                )
            },
        )
        // Add proper view to enter and validate value.
        SwitchSettings(
            title = Res.string.Settings_Websites_MaxRuntime,
            key = stringResource(Res.string.max_runtime),
            checked = false,
            onCheckedChange = { key, value ->
                onEvent(
                    SettingsCategoryViewModel.Event.CheckedChangeClick(
                        key,
                        value,
                    ),
                )
            },
        )
        SettingsDescription(
            Res.string.Settings_AutomatedTesting_RunAutomatically_Footer,
        )
    }
}

@Composable
fun PrivacySettingsScreen(onEvent: (SettingsCategoryViewModel.Event) -> Unit) {
    Column {
        SwitchSettings(
            title = Res.string.Settings_Sharing_UploadResults,
            key = stringResource(Res.string.upload_results),
            checked = false,
            onCheckedChange = { key, value ->
                onEvent(
                    SettingsCategoryViewModel.Event.CheckedChangeClick(
                        key,
                        value,
                    ),
                )
            },
        )
        SwitchSettings(
            title = Res.string.Settings_Privacy_SendCrashReports,
            key = stringResource(Res.string.send_crash),
            checked = false,
            onCheckedChange = { key, value ->
                onEvent(
                    SettingsCategoryViewModel.Event.CheckedChangeClick(
                        key,
                        value,
                    ),
                )
            },
        )
    }
}

@Composable
fun SwitchSettings(
    title: StringResource,
    supportingContent: @Composable (() -> Unit)? = null,
    key: String,
    checked: Boolean,
    onCheckedChange: (String, Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = supportingContent,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = { newValue -> onCheckedChange(key, newValue) },
            )
        },
    )
}

@Composable
fun SettingsDescription(description: StringResource) {
    Text(stringResource(description), modifier = Modifier.padding(horizontal = 16.dp), fontSize = 12.sp)
}
