package org.ooni.probe.ui.descriptors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import ooniprobe.composeapp.generated.resources.AddDescriptor_Title
import ooniprobe.composeapp.generated.resources.Common_Collapse
import ooniprobe.composeapp.generated.resources.Common_Expand
import ooniprobe.composeapp.generated.resources.DescriptorUpdate_CheckUpdates
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Tests_Title
import ooniprobe.composeapp.generated.resources.ic_keyboard_arrow_down
import ooniprobe.composeapp.generated.resources.ic_keyboard_arrow_up
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.DescriptorType
import org.ooni.probe.data.models.DescriptorUpdateOperationState
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.shared.UpdateProgressStatus
import org.ooni.probe.ui.shared.VerticalScrollbar
import org.ooni.probe.ui.theme.AppTheme

@Composable
fun DescriptorsScreen(
    state: DescriptorsViewModel.State,
    onEvent: (DescriptorsViewModel.Event) -> Unit,
) {
    val pullRefreshState = rememberPullToRefreshState()
    Box(
        Modifier
            .pullToRefresh(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(DescriptorsViewModel.Event.FetchUpdatedDescriptors) },
                state = pullRefreshState,
                enabled = state.isRefreshEnabled && state.canPullToRefresh,
            ).background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
    ) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                title = { Text(stringResource(Res.string.Tests_Title)) },
                actions = {
                    if (OrganizationConfig.canInstallDescriptors) {
                        IconButton(onClick = { onEvent(DescriptorsViewModel.Event.AddClicked) }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(Res.string.AddDescriptor_Title),
                            )
                        }
                    }
                },
            )

            Box {
                val lazyListState = rememberLazyListState()
                LazyColumn(
                    modifier = Modifier.testTag("Descriptors-List"),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    state = lazyListState,
                ) {
                    val allSectionsHaveValues = state.sections.all { it.descriptors.any() }
                    state.sections.forEach { (type, descriptors, isCollapsed) ->
                        if (allSectionsHaveValues && descriptors.isNotEmpty()) {
                            item(type) {
                                TestDescriptorSectionTitle(
                                    type = type,
                                    isCollapsed = isCollapsed,
                                    state = state,
                                    onEvent = onEvent,
                                )
                            }
                        }
                        if (isCollapsed) return@forEach
                        items(descriptors, key = { it.key }) { descriptor ->
                            TestDescriptorItem(
                                descriptor = descriptor,
                                onClick = {
                                    onEvent(
                                        DescriptorsViewModel.Event.DescriptorClicked(descriptor),
                                    )
                                },
                                onUpdateClick = {
                                    onEvent(
                                        DescriptorsViewModel.Event.UpdateDescriptorClicked(
                                            descriptor,
                                        ),
                                    )
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
                onReviewLinkClicked = { onEvent(DescriptorsViewModel.Event.ReviewUpdatesClicked) },
                onCancelClicked = { onEvent(DescriptorsViewModel.Event.CancelUpdatesClicked) },
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
private fun TestDescriptorSectionTitle(
    type: DescriptorType,
    isCollapsed: Boolean,
    state: DescriptorsViewModel.State,
    onEvent: (DescriptorsViewModel.Event) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clickable { onEvent(DescriptorsViewModel.Event.ToggleSection(type)) }
            .padding(horizontal = 16.dp)
            .defaultMinSize(minHeight = 40.dp)
            .padding(vertical = 1.dp),
    ) {
        TestDescriptorTypeTitle(type)
        Icon(
            painterResource(
                if (isCollapsed) {
                    Res.drawable.ic_keyboard_arrow_down
                } else {
                    Res.drawable.ic_keyboard_arrow_up
                },
            ),
            contentDescription = stringResource(
                if (isCollapsed) {
                    Res.string.Common_Expand
                } else {
                    Res.string.Common_Collapse
                },
            ) + " " + stringResource(type.title),
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(16.dp),
        )
        Spacer(Modifier.weight(1f))
        if (type == DescriptorType.Installed && !state.canPullToRefresh) {
            CheckUpdatesButton(
                enabled = !state.isRefreshing,
                onEvent = onEvent,
            )
        }
    }
}

@Composable
private fun CheckUpdatesButton(
    enabled: Boolean,
    onEvent: (DescriptorsViewModel.Event) -> Unit,
) {
    TextButton(
        onClick = { onEvent(DescriptorsViewModel.Event.FetchUpdatedDescriptors) },
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
fun DashboardScreenPreview() {
    AppTheme {
        DescriptorsScreen(
            state = DescriptorsViewModel.State(),
            onEvent = {},
        )
    }
}
