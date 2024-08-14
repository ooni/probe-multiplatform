package org.ooni.probe.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.app_name
import ooniprobe.composeapp.generated.resources.logo
import ooniprobe.composeapp.generated.resources.run_tests
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ooni.probe.ui.theme.AppTheme

@Composable
fun DashboardScreen(
    state: DashboardViewModel.State,
    onEvent: (DashboardViewModel.Event) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TopAppBar(
            title = { Text(stringResource(Res.string.app_name)) },
        )

        Image(
            painterResource(Res.drawable.logo),
            contentDescription = stringResource(Res.string.app_name),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
        )

        Button(
            onClick = { onEvent(DashboardViewModel.Event.StartClick) },
            enabled = !state.isRunning,
        ) {
            Text(stringResource(Res.string.run_tests))
        }

        LazyColumn {
            val allSectionsHaveValues = state.tests.entries.all { it.value.any() }
            state.tests.forEach { (type, tests) ->
                if (allSectionsHaveValues && tests.isNotEmpty()) {
                    item(type) {
                        TestDescriptorItem(type)
                    }
                }
                items(tests) { test ->
                    TestDescriptorItem(test)
                }
            }
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
