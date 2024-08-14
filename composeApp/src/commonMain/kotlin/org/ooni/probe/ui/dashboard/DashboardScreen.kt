package org.ooni.probe.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.app_name
import ooniprobe.composeapp.generated.resources.logo
import ooniprobe.composeapp.generated.resources.run_tests
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ooni.probe.data.models.TestState
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

        if (state.testState is TestState.Running) {
            Text(
                text = state.testState.testType?.let { stringResource(it.labelRes) }.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = state.testState.log.orEmpty(),
            )
            state.testState.testProgress.let { progress ->
                if (progress == 0.0) {
                    LinearProgressIndicator(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(16.dp),
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { progress.toFloat() },
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(16.dp),
                    )
                }
            }
            Text(
                text = state.testState.estimatedTimeLeft?.toString().orEmpty(),
            )
        } else {
            Button(
                onClick = { onEvent(DashboardViewModel.Event.StartClick) },
            ) {
                Text(stringResource(Res.string.run_tests))
            }
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
    }
}

@Preview
@Composable
fun DashboardScreenPreview() {
    AppTheme {
        DashboardScreen(
            state = DashboardViewModel.State(),
            onEvent = {},
        )
    }
}
