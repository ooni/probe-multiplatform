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
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Stopping_Notice
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Stopping_Title
import ooniprobe.composeapp.generated.resources.Modal_Error_CantDownloadURLs
import ooniprobe.composeapp.generated.resources.Notification_StopTest
import ooniprobe.composeapp.generated.resources.OONIRun_Run
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.app_name
import ooniprobe.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ooni.probe.LocalSnackbarHostState
import org.ooni.probe.data.models.TestRunError
import org.ooni.probe.data.models.TestRunState
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

        when (state.testRunState) {
            TestRunState.Idle -> {
                Button(
                    onClick = { onEvent(DashboardViewModel.Event.RunTestsClick) },
                ) {
                    Text(stringResource(Res.string.OONIRun_Run))
                }
            }

            is TestRunState.Running -> {
                Text(
                    text = state.testRunState.testType?.let { stringResource(it.labelRes) }
                        .orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = state.testRunState.log.orEmpty(),
                )
                state.testRunState.testProgress.let { progress ->
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
                    text = state.testRunState.estimatedTimeLeft?.toString().orEmpty(),
                )
                Button(
                    onClick = { onEvent(DashboardViewModel.Event.StopTestsClick) },
                ) {
                    Text(stringResource(Res.string.Notification_StopTest))
                }
            }

            TestRunState.Stopping -> {
                Text(
                    text = stringResource(Res.string.Dashboard_Running_Stopping_Title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(Res.string.Dashboard_Running_Stopping_Notice),
                )
            }
        }

        LazyColumn {
            val allSectionsHaveValues = state.tests.entries.all { it.value.any() }
            state.tests.forEach { (type, tests) ->
                if (allSectionsHaveValues && tests.isNotEmpty()) {
                    item(type) {
                        TestDescriptorSection(type)
                    }
                }
                items(tests) { test ->
                    TestDescriptorItem(test)
                }
            }
        }
    }

    ErrorMessages(state, onEvent)
}

@Composable
private fun ErrorMessages(
    state: DashboardViewModel.State,
    onEvent: (DashboardViewModel.Event) -> Unit,
) {
    val snackbarHostState = LocalSnackbarHostState.current ?: return
    val errorMessage = when (state.testRunErrors.firstOrNull()) {
        TestRunError.DownloadUrlsFailed -> stringResource(Res.string.Modal_Error_CantDownloadURLs)
        null -> ""
    }
    LaunchedEffect(state.testRunErrors) {
        val error = state.testRunErrors.firstOrNull() ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(errorMessage)
        if (result == SnackbarResult.Dismissed) {
            onEvent(DashboardViewModel.Event.ErrorDisplayed(error))
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
