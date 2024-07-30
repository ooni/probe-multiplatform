package org.ooni.probe.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ooni.probe.config.Config
import org.ooni.probe.ui.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardViewModel.State,
    onEvent: (DashboardViewModel.Event) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(Config.BASE_SOFTWARE_NAME)
                },
            )
        },
    ) { contentPadding ->

        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.Center,
        ) {
            Button(
                onClick = { onEvent(DashboardViewModel.Event.StartClick) },
                enabled = !state.isRunning,
            ) {
                // Text(stringResource(Res.string.run_tests))
                Text("Run Tests")
            }

//            Image(
//                painterResource(Res.drawable.logo),
//                contentDescription = "OONI Probe Logo",
//                modifier = Modifier.align(Alignment.CenterHorizontally),
//            )
            Text(
                text = state.log,
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            )
        }
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
