package org.ooni.probe.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Title
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.shared.VerticalScrollbar

@Composable
fun SettingsScreen(
    state: SettingsViewModel.State,
    onEvent: (SettingsViewModel.Event) -> Unit,
) {
    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        TopBar(
            title = {
                Text(stringResource(Res.string.Settings_Title))
            },
        )

        Box(Modifier.fillMaxSize()) {
            val scrollState = rememberScrollState()
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                state.settings.forEach { item ->
                    SettingsItemView(
                        icon = item.icon,
                        title = item.title,
                        modifier = Modifier.testTag(item.route.value).clickable {
                            onEvent(
                                SettingsViewModel.Event.SettingsCategoryClick(
                                    item.route,
                                ),
                            )
                        },
                    )
                }
            }
            VerticalScrollbar(
                state = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd),
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
