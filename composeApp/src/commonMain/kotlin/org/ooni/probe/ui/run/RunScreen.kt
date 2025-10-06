package org.ooni.probe.ui.run

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Common_Collapse
import ooniprobe.composeapp.generated.resources.Common_Expand
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_Description
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_RunButton_Label
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_SelectAll
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_SelectNone
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_Title
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.ic_keyboard_arrow_down
import ooniprobe.composeapp.generated.resources.ic_keyboard_arrow_up
import ooniprobe.composeapp.generated.resources.ic_timer
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TestType
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.config.TestDisplayMode
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.ui.dashboard.TestDescriptorLabel
import org.ooni.probe.ui.dashboard.TestDescriptorTypeTitle
import org.ooni.probe.ui.shared.DisableVpnInstructionsDialog
import org.ooni.probe.ui.shared.NavigationBackButton
import org.ooni.probe.ui.shared.ParentSelectableItem
import org.ooni.probe.ui.shared.SelectableItem
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.shared.VerticalScrollbar
import org.ooni.probe.ui.shared.VpnWarningDialog

@Composable
fun RunScreen(
    state: RunViewModel.State,
    onEvent: (RunViewModel.Event) -> Unit,
) {
    var showVpnWarning by remember { mutableStateOf(false) }

    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        TopBar(
            title = { Text(stringResource(Res.string.Dashboard_RunTests_Title)) },
            navigationIcon = {
                NavigationBackButton({ onEvent(RunViewModel.Event.BackClicked) })
            },
        )

        FlowRow(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                stringResource(Res.string.Dashboard_RunTests_Description),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(end = 16.dp).align(Alignment.CenterVertically),
            )
            Row {
                OutlinedButton(
                    onClick = { onEvent(RunViewModel.Event.SelectAllClicked) },
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    Text(stringResource(Res.string.Dashboard_RunTests_SelectAll))
                }
                OutlinedButton(
                    onClick = { onEvent(RunViewModel.Event.DeselectAllClicked) },
                ) {
                    Text(stringResource(Res.string.Dashboard_RunTests_SelectNone))
                }
            }
        }

        Box {
            val lazyListState = rememberLazyListState()
            LazyColumn(
                contentPadding = PaddingValues(
                    // Insets + Run tests button
                    bottom =
                        WindowInsets.navigationBars
                            .asPaddingValues()
                            .calculateBottomPadding() + 72.dp,
                ),
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("Run-DescriptorsList"),
            ) {
                val allSectionsHaveValues = state.list.entries.all { it.value.any() }
                state.list.forEach { (type, descriptorsMap) ->
                    if (allSectionsHaveValues && descriptorsMap.isNotEmpty()) {
                        item(key = type) {
                            TestDescriptorTypeTitle(
                                type = type,
                                modifier = Modifier
                                    .padding(top = 8.dp, start = 16.dp, bottom = 4.dp),
                            )
                        }
                    }

                    descriptorsMap.forEach descriptorsMap@{ (descriptorItem, testItems) ->
                        val descriptor = descriptorItem.item
                        item(key = descriptor.key) {
                            DescriptorItem(
                                descriptorItem = descriptorItem,
                                isExpandable = !testItems.isSingleWebConnectivityTest(),
                                onDropdownToggled = {
                                    onEvent(RunViewModel.Event.DescriptorDropdownToggled(descriptor))
                                },
                                onChecked = {
                                    onEvent(RunViewModel.Event.DescriptorChecked(descriptor, it))
                                },
                            )
                        }

                        if (!descriptorItem.isExpanded) return@descriptorsMap

                        when (OrganizationConfig.testDisplayMode) {
                            TestDisplayMode.Regular ->
                                regularTestItems(descriptor, testItems, onEvent)

                            TestDisplayMode.WebsitesOnly ->
                                websiteItems(testItems)
                        }
                    }
                }
            }

            VerticalScrollbar(state = lazyListState, modifier = Modifier.align(Alignment.CenterEnd))

            val selectedTestsCount = state.list.values.sumOf {
                it.values.sumOf { items -> items.count { item -> item.isSelected } }
            }
            Button(
                onClick = {
                    if (state.showVpnWarning) {
                        showVpnWarning = true
                    } else {
                        onEvent(RunViewModel.Event.RunClicked)
                    }
                },
                enabled = selectedTestsCount > 0,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .testTag("Run-Button"),
            ) {
                Text(
                    text = pluralStringResource(
                        Res.plurals.Dashboard_RunTests_RunButton_Label,
                        selectedTestsCount,
                        selectedTestsCount,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                )
                Icon(
                    painterResource(Res.drawable.ic_timer),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }

    if (showVpnWarning) {
        VpnWarningDialog(
            onDismiss = {
                showVpnWarning = false
            },
            onRunAnyway = {
                onEvent(RunViewModel.Event.RunClicked)
                showVpnWarning = false
            },
            onRunAlways = {
                onEvent(RunViewModel.Event.RunAlwaysClicked)
                showVpnWarning = false
            },
            onDisableVpn = {
                onEvent(RunViewModel.Event.DisableVpnClicked)
                showVpnWarning = false
            },
        )
    }

    if (state.showDisableVpnInstructions) {
        DisableVpnInstructionsDialog(
            onDismiss = { onEvent(RunViewModel.Event.DisableVpnInstructionsDismissed) },
        )
    }

    LaunchedEffect(Unit) {
        onEvent(RunViewModel.Event.Start)
    }
}

private fun LazyListScope.regularTestItems(
    descriptor: Descriptor,
    testItems: List<SelectableItem<NetTest>>,
    onEvent: (RunViewModel.Event) -> Unit,
) {
    if (testItems.isSingleWebConnectivityTest()) return

    items(testItems, key = { "${descriptor.key}_${it.item.test.name}" }) { testItem ->
        TestItem(
            testItem = testItem,
            enabled = descriptor.enabled,
            onChecked = {
                onEvent(
                    RunViewModel.Event.NetTestChecked(
                        descriptor,
                        testItem.item,
                        it,
                    ),
                )
            },
        )
    }
}

private fun List<SelectableItem<NetTest>>.isSingleWebConnectivityTest() = size == 1 && first().item.test == TestType.WebConnectivity

@Composable
private fun DescriptorItem(
    descriptorItem: ParentSelectableItem<Descriptor>,
    isExpandable: Boolean,
    onDropdownToggled: () -> Unit,
    onChecked: (Boolean) -> Unit,
) {
    val descriptor = descriptorItem.item
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .triStateToggleable(
                state = descriptorItem.state,
                onClick = { onChecked(descriptorItem.state != ToggleableState.On) },
                role = Role.Checkbox,
                enabled = descriptor.enabled,
            ).defaultMinSize(minHeight = 48.dp),
    ) {
        TriStateCheckbox(
            state = descriptorItem.state,
            enabled = descriptor.enabled,
            onClick = null,
            modifier = Modifier.padding(start = 24.dp, end = 16.dp),
        )

        TestDescriptorLabel(descriptor, modifier = Modifier.weight(1f))

        if (isExpandable) {
            IconButton(
                onClick = { onDropdownToggled() },
                modifier = Modifier.size(width = 64.dp, height = 48.dp),
            ) {
                Icon(
                    painterResource(
                        if (descriptorItem.isExpanded) {
                            Res.drawable.ic_keyboard_arrow_up
                        } else {
                            Res.drawable.ic_keyboard_arrow_down
                        },
                    ),
                    contentDescription = stringResource(
                        if (descriptorItem.isExpanded) {
                            Res.string.Common_Collapse
                        } else {
                            Res.string.Common_Expand
                        },
                    ) + " " + descriptor.title(),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
fun TestItem(
    testItem: SelectableItem<NetTest>,
    enabled: Boolean = true,
    onChecked: (Boolean) -> Unit,
) {
    val test = testItem.item
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp)
            .toggleable(
                value = testItem.isSelected,
                onValueChange = { onChecked(it) },
                role = Role.Checkbox,
                enabled = enabled,
            ).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Checkbox(
            checked = testItem.isSelected,
            enabled = enabled,
            onCheckedChange = null,
            modifier = Modifier.padding(end = 24.dp),
        )
        Text(
            if (test.test is TestType.Experimental) {
                test.test.name
            } else {
                stringResource(testItem.item.test.labelRes)
            },
        )
    }
}

private fun LazyListScope.websiteItems(testItems: List<SelectableItem<NetTest>>) {
    val websites = testItems.flatMap { it.item.inputs.orEmpty() }
    items(websites) { website ->
        Text(
            website,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, top = 4.dp),
        )
    }
}
