package org.ooni.probe.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Dashboard_Overview_LatestTest
import ooniprobe.composeapp.generated.resources.Dashboard_RunV2_RunFinished
import ooniprobe.composeapp.generated.resources.Dashboard_Running_EstimatedTimeLeft
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Running
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Stopping_Notice
import ooniprobe.composeapp.generated.resources.Dashboard_Running_Stopping_Title
import ooniprobe.composeapp.generated.resources.OONIRun_Run
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.app_name
import ooniprobe.composeapp.generated.resources.ic_timer
import ooniprobe.composeapp.generated.resources.logo_probe
import ooniprobe.composeapp.generated.resources.ooni_empty_state
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ooni.probe.data.models.TestRunState
import org.ooni.probe.ui.customColors
import org.ooni.probe.ui.shared.TestRunErrorMessages
import org.ooni.probe.ui.shared.UpdateStatus
import org.ooni.probe.ui.shared.relativeDateTime
import org.ooni.probe.ui.shared.shortFormat
import org.ooni.probe.ui.theme.AppTheme

@Composable
fun DashboardScreen(
    state: DashboardViewModel.State,
    onEvent: (DashboardViewModel.Event) -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()

    if (pullToRefreshState.isRefreshing && !state.isRefreshing) {
        onEvent(DashboardViewModel.Event.FetchUpdatedDescriptors)
    }

    if (!state.isRefreshing) {
        pullToRefreshState.endRefresh()
    }
    Box(Modifier.nestedScroll(pullToRefreshState.nestedScrollConnection)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Image(
                painterResource(Res.drawable.logo_probe),
                contentDescription = stringResource(Res.string.app_name),
                modifier = Modifier.padding(36.dp)
                    .padding(WindowInsets.statusBars.asPaddingValues()),
            )

            TestRunStateSection(state.testRunState, onEvent)

            LazyColumn(
                modifier = Modifier.padding(top = 24.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                val allSectionsHaveValues = state.descriptors.entries.all { it.value.any() }
                state.descriptors.forEach { (type, items) ->
                    if (allSectionsHaveValues && items.isNotEmpty()) {
                        item(type) {
                            TestDescriptorSection(type)
                        }
                    }
                    items(items, key = { it.key }) { descriptor ->
                        TestDescriptorItem(
                            descriptor = descriptor,
                            onClick = {
                                onEvent(DashboardViewModel.Event.DescriptorClicked(descriptor))
                            },
                        )
                    }
                }
            }
        }
        if (state.isRefreshing) {
            UpdateStatus(
                modifier = Modifier.align(Alignment.BottomCenter),
                type = state.refreshType,
                onReviewLinkClicked = { onEvent(DashboardViewModel.Event.ReviewUpdatesClicked) },
            )
        }

        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = pullToRefreshState,
        )
    }

    TestRunErrorMessages(
        errors = state.testRunErrors,
        onErrorDisplayed = { onEvent(DashboardViewModel.Event.ErrorDisplayed(it)) },
    )
}

@Composable
private fun TestRunStateSection(
    state: TestRunState,
    onEvent: (DashboardViewModel.Event) -> Unit,
) {
    when (state) {
        is TestRunState.Idle -> {
            ElevatedButton(
                onClick = { onEvent(DashboardViewModel.Event.RunTestsClick) },
                colors = ButtonDefaults.buttonColors(),
            ) {
                Text(
                    stringResource(Res.string.OONIRun_Run),
                    style = MaterialTheme.typography.titleLarge,
                )
                Icon(
                    painterResource(Res.drawable.ic_timer),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            state.lastTestAt?.let { lastTestAt ->
                Text(
                    text = stringResource(Res.string.Dashboard_Overview_LatestTest) + " " + lastTestAt.relativeDateTime(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (state.justFinishedTest) {
                Button(
                    onClick = { onEvent(DashboardViewModel.Event.SeeResultsClick) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.customColors.success,
                        contentColor = MaterialTheme.customColors.onSuccess,
                    ),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(stringResource(Res.string.Dashboard_RunV2_RunFinished))
                }
            }
        }

        is TestRunState.Running -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onEvent(DashboardViewModel.Event.RunningTestClick) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                state.testType?.let { testType ->
                    Row {
                        Text(
                            text = stringResource(Res.string.Dashboard_Running_Running),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Icon(
                            painterResource(testType.iconRes ?: Res.drawable.ooni_empty_state),
                            contentDescription = null,
                            modifier = Modifier.padding(horizontal = 4.dp).size(24.dp),
                        )
                        Text(
                            text = stringResource(testType.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                state.progress.let { progress ->
                    val color = MaterialTheme.colorScheme.primary
                    val trackColor = MaterialTheme.colorScheme.onBackground
                    val modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(8.dp)

                    if (progress == 0.0) {
                        LinearProgressIndicator(
                            color = color,
                            trackColor = trackColor,
                            modifier = modifier,
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { progress.toFloat() },
                            color = color,
                            trackColor = trackColor,
                            modifier = modifier,
                        )
                    }
                }

                state.estimatedTimeLeft?.let { timeLeft ->
                    Row {
                        Text(
                            text = stringResource(Res.string.Dashboard_Running_EstimatedTimeLeft),
                        )
                        Text(
                            text = " " + timeLeft.shortFormat(),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        TestRunState.Stopping -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.Dashboard_Running_Stopping_Title),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(Res.string.Dashboard_Running_Stopping_Notice),
                    textAlign = TextAlign.Center,
                )
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
