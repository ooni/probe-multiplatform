package org.ooni.probe.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Modal_DisableVPN_Title
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.app_name
import ooniprobe.composeapp.generated.resources.dashboard_arc
import ooniprobe.composeapp.generated.resources.ic_warning
import ooniprobe.composeapp.generated.resources.logo_probe
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ooni.probe.data.models.DescriptorUpdateOperationState
import org.ooni.probe.ui.shared.IgnoreBatteryOptimizationDialog
import org.ooni.probe.ui.shared.TestRunErrorMessages
import org.ooni.probe.ui.shared.UpdateProgressStatus
import org.ooni.probe.ui.shared.isHeightCompact
import org.ooni.probe.ui.theme.AppTheme

@Composable
fun DashboardScreen(
    state: DashboardViewModel.State,
    onEvent: (DashboardViewModel.Event) -> Unit,
) {
    val pullRefreshState = rememberPullToRefreshState()
    Box(
        Modifier
            .pullToRefresh(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(DashboardViewModel.Event.FetchUpdatedDescriptors) },
                state = pullRefreshState,
                enabled = state.isRefreshEnabled,
            )
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(bottom = if (state.isRefreshing) 48.dp else 0.dp)
                .fillMaxWidth(),
        ) {
            // Colorful top background
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .height(if (isHeightCompact()) 64.dp else 112.dp),
            ) {
                Image(
                    painterResource(Res.drawable.logo_probe),
                    contentDescription = stringResource(Res.string.app_name),
                    modifier = Modifier
                        .padding(vertical = if (isHeightCompact()) 4.dp else 20.dp)
                        .align(Alignment.Center)
                        .height(if (isHeightCompact()) 48.dp else 72.dp),
                )
            }

            Box {
                Image(
                    painterResource(Res.drawable.dashboard_arc),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    RunBackgroundStateSection(state.runBackgroundState, onEvent)
                }
            }

            if (state.showVpnWarning) {
                VpnWarning()
            }

            LazyColumn(
                modifier = Modifier.padding(top = if (isHeightCompact()) 8.dp else 24.dp)
                    .testTag("Dashboard-List"),
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
                            onUpdateClick = {
                                onEvent(DashboardViewModel.Event.UpdateDescriptorClicked(descriptor))
                            },
                        )
                    }
                }
            }
        }

        if (state.descriptorsUpdateOperationState != DescriptorUpdateOperationState.Idle) {
            UpdateProgressStatus(
                modifier = Modifier.align(Alignment.BottomCenter),
                type = state.descriptorsUpdateOperationState,
                onReviewLinkClicked = { onEvent(DashboardViewModel.Event.ReviewUpdatesClicked) },
                onCancelClicked = { onEvent(DashboardViewModel.Event.CancelUpdatesClicked) },
            )
        }

        PullToRefreshDefaults.Indicator(
            modifier = Modifier.align(Alignment.TopCenter),
            isRefreshing = state.isRefreshing,
            state = pullRefreshState,
        )
    }

    TestRunErrorMessages(
        errors = state.testRunErrors,
        onErrorDisplayed = { onEvent(DashboardViewModel.Event.ErrorDisplayed(it)) },
    )

    if (state.showIgnoreBatteryOptimizationNotice) {
        IgnoreBatteryOptimizationDialog(
            onAccepted = { onEvent(DashboardViewModel.Event.IgnoreBatteryOptimizationAccepted) },
            onDismissed = { onEvent(DashboardViewModel.Event.IgnoreBatteryOptimizationDismissed) },
        )
    }

    LaunchedEffect(Unit) {
        onEvent(DashboardViewModel.Event.Start)
    }
}

@Composable
private fun VpnWarning() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(Modifier.padding(8.dp)) {
            Icon(
                painterResource(Res.drawable.ic_warning),
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(stringResource(Res.string.Modal_DisableVPN_Title))
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
