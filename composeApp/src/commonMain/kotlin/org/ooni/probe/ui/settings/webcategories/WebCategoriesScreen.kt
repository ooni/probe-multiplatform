package org.ooni.probe.ui.settings.webcategories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import ooniprobe.composeapp.generated.resources.Common_Back
import ooniprobe.composeapp.generated.resources.Common_DeselectAll
import ooniprobe.composeapp.generated.resources.Common_SelectAll
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Websites_Categories_Label
import ooniprobe.composeapp.generated.resources.ic_deselect
import ooniprobe.composeapp.generated.resources.ic_select_all
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.ui.settings.category.SwitchSettingsView
import org.ooni.probe.ui.shared.TopBar

@Composable
fun WebCategoriesScreen(
    state: WebCategoriesViewModel.State,
    onEvent: (WebCategoriesViewModel.Event) -> Unit,
) {
    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        TopBar(
            title = {
                Text(
                    stringResource(Res.string.Settings_Websites_Categories_Label),
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 18.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = { onEvent(WebCategoriesViewModel.Event.BackClicked) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.Common_Back),
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { onEvent(WebCategoriesViewModel.Event.SelectAllClicked) },
                    enabled = state.selectAllEnabled,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_select_all),
                        stringResource(Res.string.Common_SelectAll),
                    )
                }
                IconButton(
                    onClick = { onEvent(WebCategoriesViewModel.Event.DeselectAllClicked) },
                    enabled = state.deselectAllEnabled,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_deselect),
                        stringResource(Res.string.Common_DeselectAll),
                    )
                }
            },
        )

        LazyColumn(
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        ) {
            items(state.items, key = { it.item.settingsKey!! }) { item ->
                SwitchSettingsView(
                    icon = item.item.icon,
                    title = item.item.title,
                    key = item.item.settingsKey!!,
                    checked = item.isSelected,
                    enabled = true,
                    supportingContent = { Text(stringResource(item.item.description)) },
                    onCheckedChange = { _, value ->
                        onEvent(WebCategoriesViewModel.Event.PreferenceChanged(item.item, value))
                    },
                )
            }
        }
    }
}
