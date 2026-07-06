package org.ooni.probe.ui.settings.language

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Language_Label
import ooniprobe.composeapp.generated.resources.Settings_Language_SystemDefault
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.ui.shared.NavigationBackButton
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.shared.VerticalScrollbar

@Composable
fun LanguageScreen(
    state: LanguageViewModel.State,
    onEvent: (LanguageViewModel.Event) -> Unit,
) {
    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        TopBar(
            title = { Text(stringResource(Res.string.Settings_Language_Label)) },
            navigationIcon = {
                NavigationBackButton({ onEvent(LanguageViewModel.Event.BackClicked) })
            },
        )
        Box(Modifier.fillMaxSize()) {
            val lazyListState = rememberLazyListState()
            LazyColumn(
                state = lazyListState,
                contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                modifier = Modifier.selectableGroup(),
            ) {
                item(key = "system_default") {
                    LanguageRow(
                        label = stringResource(Res.string.Settings_Language_SystemDefault),
                        isSelected = state.selectedLanguage == null,
                        onClick = { onEvent(LanguageViewModel.Event.OptionSelected(null)) },
                    )
                }
                items(state.options, key = { it.code }) { option ->
                    LanguageRow(
                        label = option.name,
                        isSelected = state.selectedLanguage == option.code,
                        onClick = { onEvent(LanguageViewModel.Event.OptionSelected(option.code)) },
                    )
                }
            }
            VerticalScrollbar(state = lazyListState, modifier = Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
private fun LanguageRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp).weight(1f),
        )
    }
    HorizontalDivider(thickness = Dp.Hairline)
}
