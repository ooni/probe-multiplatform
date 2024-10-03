package org.ooni.probe.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Title
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
    state: SettingsViewModel.State,
    onEvent: (SettingsViewModel.Event) -> Unit,
) {
    Column {
        TopAppBar(
            title = {
                Text(stringResource(Res.string.Settings_Title))
            },
        )

        state.settings.forEach { item ->
            SettingsItemView(
                icon = item.icon,
                title = item.title,
                modifier = Modifier.clickable {
                    onEvent(
                        SettingsViewModel.Event.SettingsCategoryClick(
                            item.route,
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun SettingsItemView(
    icon: DrawableResource?,
    title: StringResource,
    modifier: Modifier,
) {
    ListItem(
        leadingContent = {
            icon?.let {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(it),
                    contentDescription = stringResource(title),
                )
            }
        },
        headlineContent = { Text(stringResource(title)) },
        modifier = modifier,
    )
}
