package org.ooni.probe.ui.descriptor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.AddDescriptor_AutoUpdate
import ooniprobe.composeapp.generated.resources.Dashboard_Runv2_Overview_PreviousRevisions
import ooniprobe.composeapp.generated.resources.Dashboard_Runv2_Overview_SeeMore
import ooniprobe.composeapp.generated.resources.Dashboard_Runv2_Overview_UninstallLink
import ooniprobe.composeapp.generated.resources.Dashboard_Runv2_Overview_Uninstall_Prompt
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Modal_CustomURL_Title_NotSaved
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.InstalledTestDescriptorModel

@Composable
fun InstalledDescriptorActionsView(
    descriptor: InstalledTestDescriptorModel,
    onEvent: (DescriptorViewModel.Event) -> Unit,
    modifier: Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(Res.string.Modal_CustomURL_Title_NotSaved)) },
            text = { Text(stringResource(Res.string.Dashboard_Runv2_Overview_Uninstall_Prompt)) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    onClick = {
                        onEvent(DescriptorViewModel.Event.UninstallClicked(descriptor))
                        showDialog = false
                    },
                ) {
                    Text(stringResource(Res.string.Dashboard_Runv2_Overview_UninstallLink))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDialog = false },
                ) {
                    Text(stringResource(Res.string.Modal_Cancel))
                }
            },
        )
    }

    Column(modifier = modifier.padding(top = 16.dp)) {
        val revisions = descriptor.previousRevisions

        if (revisions.isNotEmpty()) {
            Text(text = stringResource(Res.string.Dashboard_Runv2_Overview_PreviousRevisions))

            revisions.take(5).forEach { revision ->
                TextButton(
                    onClick = { onEvent(DescriptorViewModel.Event.RevisionClicked(revision)) },
                ) {
                    Text(text = "#: $revision")
                }
            }
            if (revisions.size > 5) {
                TextButton(
                    onClick = { onEvent(DescriptorViewModel.Event.SeeMoreRevisionsClicked) },
                ) {
                    Text(text = stringResource(Res.string.Dashboard_Runv2_Overview_SeeMore))
                }
            }
        }

        Button(
            onClick = { showDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = Color.White,
            ),
        ) {
            Text(text = stringResource(Res.string.Dashboard_Runv2_Overview_UninstallLink))
        }
    }
}

@Composable
fun ConfigureUpdates(
    onEvent: (DescriptorViewModel.Event) -> Unit,
    autoUpdate: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(bottom = 8.dp)
            .toggleable(
                value = autoUpdate,
                onValueChange = { onEvent(DescriptorViewModel.Event.AutoUpdateChanged(it)) },
                role = Role.Switch,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            stringResource(Res.string.AddDescriptor_AutoUpdate),
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = autoUpdate,
            onCheckedChange = null,
        )
    }
}
