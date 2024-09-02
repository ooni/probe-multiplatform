package org.ooni.probe.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import org.ooni.probe.data.models.SettingsCategoryItem

@Composable
fun SettingsScreen(onNavigateToSettingsCategory: (SettingsViewModel.Event) -> Unit) {
    Column(
        modifier = Modifier.padding(WindowInsets.navigationBars.asPaddingValues()),
    ) {
        TopAppBar(
            title = {
                Text(stringResource(Res.string.Settings_Title))
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
