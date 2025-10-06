package org.ooni.probe.ui.descriptor.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.AddDescriptor_AutoRun
import ooniprobe.composeapp.generated.resources.AddDescriptor_AutoUpdate
import ooniprobe.composeapp.generated.resources.AddDescriptor_InstallAndRun
import ooniprobe.composeapp.generated.resources.AddDescriptor_InstallForLater
import ooniprobe.composeapp.generated.resources.AddDescriptor_Settings
import ooniprobe.composeapp.generated.resources.AddDescriptor_Title
import ooniprobe.composeapp.generated.resources.LoadingScreen_Runv2_Message
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.ic_timer
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.toDescriptor
import org.ooni.probe.ui.dashboard.TestDescriptorLabel
import org.ooni.probe.ui.descriptor.isSingleWebConnectivityTest
import org.ooni.probe.ui.run.TestItem
import org.ooni.probe.ui.shared.NavigationCloseButton
import org.ooni.probe.ui.shared.NotificationMessages
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.shared.VerticalScrollbar

@Composable
fun AddDescriptorScreen(
    state: AddDescriptorViewModel.State,
    onEvent: (AddDescriptorViewModel.Event) -> Unit,
) {
    state.descriptor?.let { descriptor ->
        Column(Modifier.background(MaterialTheme.colorScheme.background)) {
            TopBar(
                title = { Text(stringResource(Res.string.AddDescriptor_Title)) },
                actions = {
                    NavigationCloseButton(
                        onClick = { onEvent(AddDescriptorViewModel.Event.CancelClicked) },
                        contentDescription = Res.string.Modal_Cancel,
                    )
                },
            )

            Box(Modifier.weight(1f)) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 36.dp),
                ) {
                    Box(modifier = Modifier.padding(vertical = 24.dp)) {
                        TestDescriptorLabel(descriptor.toDescriptor())
                    }
                    descriptor.toDescriptor().shortDescription()?.let { shortDescription ->
                        Text(shortDescription)
                    }
                    Text(
                        stringResource(Res.string.AddDescriptor_Settings),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = state.autoUpdate,
                                onValueChange = {
                                    onEvent(AddDescriptorViewModel.Event.AutoUpdateChanged(it))
                                },
                                role = Role.Switch,
                            ).padding(vertical = 8.dp),
                    ) {
                        Text(
                            stringResource(Res.string.AddDescriptor_AutoUpdate),
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = state.autoUpdate,
                            onCheckedChange = null,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .triStateToggleable(
                                state = state.allTestsSelected(),
                                onClick = {
                                    onEvent(
                                        AddDescriptorViewModel.Event.AutoRunChanged(
                                            state.allTestsSelected() != ToggleableState.On,
                                        ),
                                    )
                                },
                            ).padding(start = 16.dp)
                            .padding(vertical = 12.dp),
                    ) {
                        TriStateCheckbox(
                            state = state.allTestsSelected(),
                            onClick = null,
                            modifier = Modifier.padding(end = 24.dp),
                        )
                        Text(
                            stringResource(Res.string.AddDescriptor_AutoRun),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        state.selectableItems.forEach { selectableItem ->
                            TestItem(selectableItem, onChecked = { _ ->
                                onEvent(
                                    AddDescriptorViewModel.Event.SelectableItemClicked(
                                        selectableItem,
                                    ),
                                )
                            })
                        }

                        if (state.selectableItems.isSingleWebConnectivityTest()) {
                            state.selectableItems
                                .first()
                                .item.inputs
                                .orEmpty()
                                .forEach { website ->
                                    Text(
                                        text = website,
                                        modifier = Modifier.padding(start = 46.dp, top = 4.dp),
                                        maxLines = 1,
                                    )
                                }
                        }
                    }
                }
                VerticalScrollbar(
                    state = scrollState,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }

            Row(
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 24.dp)
                    .padding(horizontal = 16.dp)
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    onClick = { onEvent(AddDescriptorViewModel.Event.InstallClicked) },
                ) {
                    Text(stringResource(Res.string.AddDescriptor_InstallForLater))
                }

                Button(
                    onClick = { onEvent(AddDescriptorViewModel.Event.RunClicked) },
                ) {
                    Text(
                        stringResource(Res.string.AddDescriptor_InstallAndRun),
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
    } ?: LoadingDescriptor()

    NotificationMessages(
        message = state.messages,
        onMessageDisplayed = {
            onEvent(AddDescriptorViewModel.Event.MessageDisplayed(it))
        },
    )
}

@Composable
private fun LoadingDescriptor() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text(stringResource(Res.string.LoadingScreen_Runv2_Message))
        }
    }
}
