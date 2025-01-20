package org.ooni.probe.ui.descriptor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.AddDescriptor_AutoRun
import ooniprobe.composeapp.generated.resources.AddDescriptor_Settings
import ooniprobe.composeapp.generated.resources.Common_Back
import ooniprobe.composeapp.generated.resources.Dashboard_Overview_ChooseWebsites
import ooniprobe.composeapp.generated.resources.Dashboard_Overview_Estimated
import ooniprobe.composeapp.generated.resources.Dashboard_Overview_LastRun_Never
import ooniprobe.composeapp.generated.resources.Dashboard_Overview_LatestTest
import ooniprobe.composeapp.generated.resources.Dashboard_Runv2_Overview_ReviewUpdates
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.ooni_empty_state
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TestType
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.config.TestDisplayMode
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.UpdateStatus
import org.ooni.probe.ui.shared.ExpiredChip
import org.ooni.probe.ui.shared.MarkdownViewer
import org.ooni.probe.ui.shared.SelectableItem
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.shared.UpdateProgressStatus
import org.ooni.probe.ui.shared.UpdatesChip
import org.ooni.probe.ui.shared.isHeightCompact
import org.ooni.probe.ui.shared.relativeDateTime
import org.ooni.probe.ui.shared.shortFormat
import org.ooni.probe.ui.theme.LocalCustomColors

@Composable
fun DescriptorScreen(
    state: DescriptorViewModel.State,
    onEvent: (DescriptorViewModel.Event) -> Unit,
) {
    val pullRefreshState = rememberPullToRefreshState()
    Box(
        Modifier
            .pullToRefresh(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(DescriptorViewModel.Event.FetchUpdatedDescriptor) },
                state = pullRefreshState,
                enabled = state.descriptor?.source is Descriptor.Source.Installed,
            )
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column {
            val descriptorColor = state.descriptor?.color ?: MaterialTheme.colorScheme.primary
            val onDescriptorColor = LocalCustomColors.current.onDescriptor
            TopBar(
                title = {
                    Text(state.descriptor?.title?.invoke().orEmpty())
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(DescriptorViewModel.Event.BackClicked) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.Common_Back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = descriptorColor,
                    scrolledContainerColor = descriptorColor,
                    navigationIconContentColor = onDescriptorColor,
                    titleContentColor = onDescriptorColor,
                    actionIconContentColor = onDescriptorColor,
                ),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .padding(bottom = 32.dp),
            ) {
                val descriptor = state.descriptor ?: return

                DescriptorDetails(state, onEvent)

                descriptor.metadata()?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    )
                }
                MarkdownViewer(
                    markdown = descriptor.description().orEmpty(),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                )

                if (descriptor.source is Descriptor.Source.Installed) {
                    ConfigureUpdates(onEvent, descriptor.source.value.autoUpdate)
                }
                Text(
                    stringResource(Res.string.AddDescriptor_Settings),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .triStateToggleable(
                            state = state.allState,
                            onClick = { onEvent(DescriptorViewModel.Event.AllChecked) },
                            role = Role.Checkbox,
                        )
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                ) {
                    TriStateCheckbox(
                        state = state.allState,
                        onClick = null,
                        modifier = Modifier.padding(start = 16.dp, end = 24.dp),
                    )
                    Text(stringResource(Res.string.AddDescriptor_AutoRun))
                }

                when (OrganizationConfig.testDisplayMode) {
                    TestDisplayMode.Regular -> TestItems(descriptor, state.tests, onEvent)
                    TestDisplayMode.WebsitesOnly -> WebsiteItems(state.tests)
                }

                if (descriptor.source is Descriptor.Source.Installed) {
                    InstalledDescriptorActionsView(
                        descriptor = descriptor.source.value,
                        onEvent = onEvent,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
        if (state.isRefreshing) {
            UpdateProgressStatus(
                modifier = Modifier.align(Alignment.BottomCenter),
                contentModifier = Modifier.padding(WindowInsets.navigationBars.asPaddingValues()),
                type = state.refreshType,
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
private fun DescriptorDetails(
    state: DescriptorViewModel.State,
    onEvent: (DescriptorViewModel.Event) -> Unit,
) {
    val descriptor = state.descriptor ?: return
    val descriptorColor = descriptor.color ?: MaterialTheme.colorScheme.primary
    val onDescriptorColor = LocalCustomColors.current.onDescriptor

    Surface(
        color = descriptorColor,
        contentColor = onDescriptorColor,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
                .padding(8.dp),
        ) {
            if (!isHeightCompact()) {
                descriptor.icon?.let { icon ->
                    Icon(
                        painterResource(icon),
                        contentDescription = null,
                        tint = onDescriptorColor,
                        modifier = Modifier.size(64.dp),
                    )
                }
            }

            Row {
                Text(stringResource(Res.string.Dashboard_Overview_Estimated))

                descriptor.dataUsage()?.let { dataUsage ->
                    Text(
                        text = dataUsage,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                state.estimatedTime?.let { time ->
                    Text(
                        text = "~ ${time.shortFormat()}",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            Row {
                Text(stringResource(Res.string.Dashboard_Overview_LatestTest))

                Text(
                    text = state.lastResult?.startTime?.relativeDateTime()
                        ?: stringResource(Res.string.Dashboard_Overview_LastRun_Never),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            if (descriptor.name == "websites") {
                OutlinedButton(
                    onClick = { onEvent(DescriptorViewModel.Event.ChooseWebsitesClicked) },
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = SolidColor(onDescriptorColor),
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = onDescriptorColor),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(stringResource(Res.string.Dashboard_Overview_ChooseWebsites))
                }
            }

            if (descriptor.isExpired) {
                ExpiredChip()
            }

            if (descriptor.updateStatus is UpdateStatus.UpdateRejected) {
                UpdatesChip(
                    onClick = { onEvent(DescriptorViewModel.Event.UpdateDescriptor) },
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else if (descriptor.updateStatus is UpdateStatus.Updatable) {
                OutlinedButton(
                    onClick = { onEvent(DescriptorViewModel.Event.UpdateDescriptor) },
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = SolidColor(onDescriptorColor),
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = onDescriptorColor),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(stringResource(Res.string.Dashboard_Runv2_Overview_ReviewUpdates))
                }
            }
        }
    }
}

@Composable
private fun TestItems(
    descriptor: Descriptor,
    tests: List<SelectableItem<NetTest>>,
    onEvent: (DescriptorViewModel.Event) -> Unit,
) {
    tests.forEach { netTestItem ->
        val test = netTestItem.item
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp)
                .toggleable(
                    value = netTestItem.isSelected,
                    onValueChange = { onEvent(DescriptorViewModel.Event.TestChecked(test, it)) },
                    role = Role.Checkbox,
                )
                .padding(horizontal = 16.dp)
                .padding(vertical = 12.dp),
        ) {
            Checkbox(
                checked = netTestItem.isSelected,
                onCheckedChange = null,
            )
            Icon(
                painterResource(
                    test.test.iconRes ?: descriptor.icon ?: Res.drawable.ooni_empty_state,
                ),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 24.dp, end = 8.dp)
                    .size(24.dp),
            )
            Text(
                if (test.test is TestType.Experimental) {
                    test.test.name
                } else {
                    stringResource(test.test.labelRes)
                },
            )
        }
    }
}

@Composable
private fun WebsiteItems(tests: List<SelectableItem<NetTest>>) {
    val websites = tests
        .map { it.item }
        .filter { it.test is TestType.WebConnectivity }
        .flatMap { it.inputs.orEmpty() }

    websites.forEach { website ->
        Text(
            website,
            Modifier.padding(start = 48.dp, top = 4.dp),
        )
    }
}
