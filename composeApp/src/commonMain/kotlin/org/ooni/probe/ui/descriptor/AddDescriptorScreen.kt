package org.ooni.probe.ui.descriptor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.AddDescriptor_Action
import ooniprobe.composeapp.generated.resources.AddDescriptor_AutoRun
import ooniprobe.composeapp.generated.resources.AddDescriptor_AutoUpdate
import ooniprobe.composeapp.generated.resources.AddDescriptor_Settings
import ooniprobe.composeapp.generated.resources.AddDescriptor_Title
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.toDescriptor
import org.ooni.probe.ui.dashboard.TestDescriptorLabel
import org.ooni.probe.ui.run.TestItem

@Composable
fun AddDescriptorScreen(
    state: AddDescriptorViewModel.State,
    onEvent: (AddDescriptorViewModel.Event) -> Unit,
) {
    state.descriptor?.let { descriptor ->
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
        ) {
            TopAppBar(title = {
                Text(stringResource(Res.string.AddDescriptor_Title))
            }, actions = {
                IconButton(
                    onClick = {
                        onEvent(
                            AddDescriptorViewModel.Event.CancelClicked,
                        )
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(Res.string.Modal_Cancel),
                        modifier = Modifier.rotate(45.0F),
                    )
                }
            })
            Box(modifier = Modifier.padding(16.dp)) {
                TestDescriptorLabel(descriptor.toDescriptor())
            }
            descriptor.toDescriptor().shortDescription()?.let { shortDescription ->
                Text(
                    shortDescription,
                )
            }
            Text(
                stringResource(Res.string.AddDescriptor_Settings),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable {
                    onEvent(AddDescriptorViewModel.Event.AutoUpdateChanged(!state.autoUpdate))
                },
            ) {
                Text(
                    stringResource(Res.string.AddDescriptor_AutoUpdate),
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.autoUpdate,
                    onCheckedChange = {
                        onEvent(AddDescriptorViewModel.Event.AutoUpdateChanged(it))
                    },
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable {
                    onEvent(AddDescriptorViewModel.Event.AutoRunChanged(!state.allTestsSelected()))
                },
            ) {
                Text(
                    stringResource(Res.string.AddDescriptor_AutoRun),
                    modifier = Modifier.weight(1f),
                )
                Checkbox(
                    checked = state.allTestsSelected(),
                    onCheckedChange = {
                        onEvent(AddDescriptorViewModel.Event.AutoRunChanged(it))
                    },
                )
            }
            LazyColumn {
                items(state.selectableItems) { selectableItem ->
                    TestItem(selectableItem, onChecked = { _ ->
                        onEvent(AddDescriptorViewModel.Event.SelectableItemClicked(selectableItem))
                    })
                }
            }
            Row(
                modifier = Modifier
                    .padding(top = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    onClick = {
                        onEvent(AddDescriptorViewModel.Event.CancelClicked)
                    },
                ) {
                    Text(
                        stringResource(Res.string.Modal_Cancel),
                    )
                }

                TextButton(
                    onClick = {
                        onEvent(AddDescriptorViewModel.Event.InstallDescriptorClicked)
                    },
                ) {
                    Text(
                        stringResource(Res.string.AddDescriptor_Action),
                    )
                }
            }
        }
    } ?: Column(
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Loading")
    }
}
