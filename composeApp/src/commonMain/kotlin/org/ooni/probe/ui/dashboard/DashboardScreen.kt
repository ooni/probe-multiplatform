package org.ooni.probe.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ooni.probe.ui.Theme

@Composable
fun DashboardScreen(
    state: DashboardViewModel.State,
    onEvent: (DashboardViewModel.Event) -> Unit,
) {
    Column {
        Button(
            onClick = { onEvent(DashboardViewModel.Event.StartClick) },
            enabled = !state.isRunning,
        ) {
            Text("Run Test")
        }

        Text(
            text = state.log,
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
        )
    }
}

@Preview
@Composable
fun DashboardScreenPreview() {
    Theme {
        DashboardScreen(
            state = DashboardViewModel.State(isRunning = false, log = ""),
            onEvent = {},
        )
    }
}
