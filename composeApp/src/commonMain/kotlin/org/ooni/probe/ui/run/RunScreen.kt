package org.ooni.probe.ui.run

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_Description
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_RunButton_Label
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_SelectAll
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_SelectNone
import ooniprobe.composeapp.generated.resources.Dashboard_RunTests_Title
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.back
import ooniprobe.composeapp.generated.resources.collapse
import ooniprobe.composeapp.generated.resources.expand
import ooniprobe.composeapp.generated.resources.ic_keyboard_arrow_down
import ooniprobe.composeapp.generated.resources.ic_keyboard_arrow_up
import ooniprobe.composeapp.generated.resources.ic_timer
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.ui.dashboard.TestDescriptorLabel
import org.ooni.probe.ui.dashboard.TestDescriptorSection
import org.ooni.probe.ui.shared.SelectableAndCollapsableItem
import org.ooni.probe.ui.shared.SelectableItem

@Composable
fun RunScreen(
    state: RunViewModel.State,
    onEvent: (RunViewModel.Event) -> Unit,
) {
    Column(
        modifier = Modifier.padding(WindowInsets.navigationBars.asPaddingValues()),
    ) {
        TopAppBar(
            title = { Text(stringResource(Res.string.Dashboard_RunTests_Title)) },
            navigationIcon = {
                IconButton(onClick = { onEvent(RunViewModel.Event.BackClicked) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                    )
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(
                stringResource(Res.string.Dashboard_RunTests_Description),
                style = MaterialTheme.typography.bodyLarge,
            )
            Row {
                OutlinedButton(onClick = { onEvent(RunViewModel.Event.SelectAllClicked) }) {
                    Text(stringResource(Res.string.Dashboard_RunTests_SelectAll))
                }
                OutlinedButton(onClick = { onEvent(RunViewModel.Event.DeselectAllClicked) }) {
                    Text(stringResource(Res.string.Dashboard_RunTests_SelectNone))
                }
            }
        }

        Box {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    // Insets + Run tests button
                    bottom =
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                            64.dp,
                ),
            ) {
                val allSectionsHaveValues = state.list.entries.all { it.value.any() }
                state.list.forEach { (type, descriptorsMap) ->
                    if (allSectionsHaveValues && descriptorsMap.isNotEmpty()) {
                        item(type) {
                            TestDescriptorSection(type)
                        }
                    }

                    descriptorsMap.forEach { (descriptorItem, testItems) ->
                        val descriptor = descriptorItem.item
                        item(descriptor.key) {
                            DescriptorItem(
                                descriptorItem = descriptorItem,
                                onDropdownToggled = {
                                    onEvent(RunViewModel.Event.DescriptorDropdownToggled(descriptor))
                                },
                                onChecked = {
                                    onEvent(RunViewModel.Event.DescriptorChecked(descriptor, it))
                                },
                            )
                        }

                        if (!descriptorItem.isExpanded) return@forEach

                        items(testItems) { testItem ->
                            TestItem(
                                testItem = testItem,
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
                }
            }

            val selectedTestsCount = state.list.values.sumOf {
                it.values.sumOf { items -> items.count { item -> item.isSelected } }
            }
            Button(
                onClick = { onEvent(RunViewModel.Event.RunClicked) },
                enabled = selectedTestsCount > 0,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
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
}

@Composable
private fun DescriptorItem(
    descriptorItem: SelectableAndCollapsableItem<Descriptor>,
    onDropdownToggled: () -> Unit,
    onChecked: (Boolean) -> Unit,
) {
    val descriptor = descriptorItem.item
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .clickable { onDropdownToggled() },
    ) {
        TestDescriptorLabel(descriptor)
        IconButton(onClick = { onDropdownToggled() }) {
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
                        Res.string.collapse
                    } else {
                        Res.string.expand
                    },
                ),
            )
        }
        Spacer(Modifier.weight(1f))
        Checkbox(
            checked = descriptorItem.isSelected,
            onCheckedChange = { onChecked(it) },
        )
    }
}

@Composable
fun TestItem(
    testItem: SelectableItem<NetTest>,
    onChecked: (Boolean) -> Unit,
) {
    val test = testItem.item
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .padding(start = 32.dp)
            .clickable { onChecked(!testItem.isSelected) },
    ) {
        Text(
            if (test.test is TestType.Experimental) {
                test.test.name
            } else {
                stringResource(testItem.item.test.labelRes)
            },
            modifier = Modifier.weight(1f),
        )
        Checkbox(
            checked = testItem.isSelected,
            onCheckedChange = { onChecked(it) },
        )
    }
}
