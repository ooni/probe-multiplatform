package org.ooni.probe.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.settings
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ooni.probe.ui.theme.AppTheme

@Composable
fun SettingsScreen() {
    Column {
        TopAppBar(
            title = {
                Text(stringResource(Res.string.settings))
            },
        )
    }
}

@Preview
@Composable
fun SettingsScreenPreview() {
    AppTheme {
        SettingsScreen()
    }
}
