package org.ooni.probe.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.app_name
import ooniprobe.composeapp.generated.resources.logo
import ooniprobe.composeapp.generated.resources.run_tests
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ooni.probe.ui.AppTheme

@Composable
fun DashboardScreen(
    state: DashboardViewModel.State,
    onEvent: (DashboardViewModel.Event) -> Unit,
) {
    Column {
        TopAppBar(
            title = { Text(stringResource(Res.string.app_name)) },
        )

        Image(
            painterResource(Res.drawable.logo),
            contentDescription = stringResource(Res.string.app_name),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Button(
            onClick = { onEvent(DashboardViewModel.Event.StartClick) },
            enabled = !state.isRunning,
        ) {
            Text(stringResource(Res.string.run_tests))
        }

        Text(
            text = state.log,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        )
    }
}

@Preview
@Composable
fun DashboardScreenPreview() {
    AppTheme {
        DashboardScreen(
            state = DashboardViewModel.State(isRunning = false, log = ""),
            onEvent = {},
        )
    }
}
