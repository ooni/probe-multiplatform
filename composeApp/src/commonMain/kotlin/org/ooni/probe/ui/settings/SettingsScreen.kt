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
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_About_Label
import ooniprobe.composeapp.generated.resources.Settings_Advanced_Label
import ooniprobe.composeapp.generated.resources.Settings_Notifications_Label
import ooniprobe.composeapp.generated.resources.Settings_Privacy_Label
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Label
import ooniprobe.composeapp.generated.resources.Settings_SendEmail_Label
import ooniprobe.composeapp.generated.resources.Settings_TestOptions_Label
import ooniprobe.composeapp.generated.resources.about_ooni
import ooniprobe.composeapp.generated.resources.advanced
import ooniprobe.composeapp.generated.resources.ic_settings
import ooniprobe.composeapp.generated.resources.notifications
import ooniprobe.composeapp.generated.resources.ooni_backend_proxy
import ooniprobe.composeapp.generated.resources.outline_info
import ooniprobe.composeapp.generated.resources.privacy
import ooniprobe.composeapp.generated.resources.proxy
import ooniprobe.composeapp.generated.resources.send_email
import ooniprobe.composeapp.generated.resources.settings
import ooniprobe.composeapp.generated.resources.test_options
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(onNavigateToSettingsCategory: (SettingsViewModel.Event) -> Unit) {
    Column {
        TopAppBar(
            title = {
                Text(stringResource(Res.string.settings))
            },
        )
        SettingsItem(
            icon = Res.drawable.notifications,
            title = Res.string.Settings_Notifications_Label,
            modifier =
                stringResource(Res.string.notifications).let { category ->
                    Modifier.clickable {
                        onNavigateToSettingsCategory(
                            category.routeToSettingsCategory(),
                        )
                    }
                },
        )
        SettingsItem(
            icon = Res.drawable.ic_settings,
            title = Res.string.Settings_TestOptions_Label,
            modifier =
                stringResource(Res.string.test_options).let { category ->
                    Modifier.clickable {
                        onNavigateToSettingsCategory(
                            category.routeToSettingsCategory(),
                        )
                    }
                },
        )
        SettingsItem(
            icon = Res.drawable.privacy,
            title = Res.string.Settings_Privacy_Label,
            modifier =
                stringResource(Res.string.privacy).let { category ->
                    Modifier.clickable {
                        onNavigateToSettingsCategory(
                            category.routeToSettingsCategory(),
                        )
                    }
                },
        )
        SettingsItem(
            icon = Res.drawable.proxy,
            title = Res.string.Settings_Proxy_Label,
            modifier =
                stringResource(Res.string.ooni_backend_proxy).let { category ->
                    Modifier.clickable {
                        onNavigateToSettingsCategory(
                            category.routeToSettingsCategory(),
                        )
                    }
                },
        )
        SettingsItem(
            icon = Res.drawable.advanced,
            title = Res.string.Settings_Advanced_Label,
            modifier =
                stringResource(Res.string.advanced).let { category ->
                    Modifier.clickable {
                        onNavigateToSettingsCategory(
                            category.routeToSettingsCategory(),
                        )
                    }
                },
        )
        SettingsItem(
            icon = Res.drawable.send_email,
            title = Res.string.Settings_SendEmail_Label,
            modifier =
                stringResource(Res.string.send_email).let { category ->
                    Modifier.clickable {
                        onNavigateToSettingsCategory(
                            category.routeToSettingsCategory(),
                        )
                    }
                },
        )
        SettingsItem(
            icon = Res.drawable.outline_info,
            title = Res.string.Settings_About_Label,
            modifier =
                stringResource(Res.string.about_ooni).let { category ->
                    Modifier.clickable {
                        onNavigateToSettingsCategory(
                            category.routeToSettingsCategory(),
                        )
                    }
                },
        )
    }
}

fun String.routeToSettingsCategory() = SettingsViewModel.Event.SettingsCategoryClick(this)

@Composable
fun SettingsItem(
    icon: DrawableResource,
    title: StringResource,
    modifier: Modifier,
) {
    ListItem(
        leadingContent = {
            Image(
                modifier = Modifier.height(24.dp).width(24.dp),
                painter = painterResource(icon),
                contentDescription = stringResource(title),
            )
        },
        headlineContent = { Text(stringResource(title)) },
        modifier = modifier,
    )
}
