package org.ooni.probe.ui.tests

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.DescriptorUpdate_CheckUpdates
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Tests_Title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ooni.probe.data.models.DescriptorType
import org.ooni.probe.data.models.DescriptorUpdateOperationState
import org.ooni.probe.ui.dashboard.TestDescriptorItem
import org.ooni.probe.ui.dashboard.TestDescriptorSection
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.shared.UpdateProgressStatus
import org.ooni.probe.ui.shared.VerticalScrollbar
import org.ooni.probe.ui.theme.AppTheme

@Composable
fun TestsScreen(
    state: TestsViewModel.State,
    onEvent: (TestsViewModel.Event) -> Unit,
) {
    val pullRefreshState = rememberPullToRefreshState()
    Box(
        Modifier
            .pullToRefresh(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(TestsViewModel.Event.FetchUpdatedDescriptors) },
                state = pullRefreshState,
                enabled = state.isRefreshEnabled && state.canPullToRefresh,
            ).background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = if (state.isRefreshing) 48.dp else 0.dp)
                .fillMaxWidth(),
        ) {
            TopBar(
                title = { Text(stringResource(Res.string.Tests_Title)) },
            )

            Box {
                val lazyListState = rememberLazyListState()
                LazyColumn(
                    modifier = Modifier
                        .testTag("Dashboard-List"),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    state = lazyListState,
                ) {
                    val allSectionsHaveValues = state.descriptors.entries.all { it.value.any() }
                    state.descriptors.forEach { (type, items) ->
                        if (allSectionsHaveValues && items.isNotEmpty()) {
                            item(type) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .padding(top = 16.dp, bottom = 4.dp),
                                ) {
                                    TestDescriptorSection(type, modifier = Modifier.weight(1f))
                                    if (type == DescriptorType.Installed && !state.canPullToRefresh) {
                                        CheckUpdatesButton(
                                            enabled = !state.isRefreshing,
                                            onEvent = onEvent,
                                        )
                                    }
                                }
                            }
                        }
                        items(items, key = { it.key }) { descriptor ->
                            TestDescriptorItem(
                                descriptor = descriptor,
                                onClick = {
                                    onEvent(TestsViewModel.Event.DescriptorClicked(descriptor))
                                },
                                onUpdateClick = {
                                    onEvent(TestsViewModel.Event.UpdateDescriptorClicked(descriptor))
                                },
                            )
                        }
                    }
                }
                VerticalScrollbar(
                    state = lazyListState,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }

        if (state.descriptorsUpdateOperationState != DescriptorUpdateOperationState.Idle) {
            UpdateProgressStatus(
                modifier = Modifier.align(Alignment.BottomCenter),
                type = state.descriptorsUpdateOperationState,
                onReviewLinkClicked = { onEvent(TestsViewModel.Event.ReviewUpdatesClicked) },
                onCancelClicked = { onEvent(TestsViewModel.Event.CancelUpdatesClicked) },
            )
        }

        PullToRefreshDefaults.Indicator(
            modifier = Modifier.align(Alignment.TopCenter),
            isRefreshing = state.isRefreshing,
            state = pullRefreshState,
        )
    }
}

@Composable
private fun CheckUpdatesButton(
    enabled: Boolean,
    onEvent: (TestsViewModel.Event) -> Unit,
) {
    TextButton(
        onClick = { onEvent(TestsViewModel.Event.FetchUpdatedDescriptors) },
        enabled = enabled,
        contentPadding = PaddingValues(
            horizontal = 8.dp,
            vertical = 4.dp,
        ),
        modifier = Modifier.defaultMinSize(minHeight = 32.dp),
    ) {
        Text(
            stringResource(Res.string.DescriptorUpdate_CheckUpdates),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Preview
@Composable
fun TestsScreenPreview() {
    AppTheme {
        TestsScreen(
            state = TestsViewModel.State(),
            onEvent = {},
        )
    }
}
