package org.ooni.probe.ui.shared

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import ooniprobe.composeapp.generated.resources.Common_Dismiss
import ooniprobe.composeapp.generated.resources.Dashboard_UpdatePrompt_Action
import ooniprobe.composeapp.generated.resources.Dashboard_UpdatePrompt_Description
import ooniprobe.composeapp.generated.resources.Dashboard_UpdatePrompt_Title
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource

@Composable
fun UpdateRequiredDialog(
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(Res.string.Dashboard_UpdatePrompt_Title)) },
        text = { Text(stringResource(Res.string.Dashboard_UpdatePrompt_Description)) },
        confirmButton = {
            TextButton(onClick = { onUpdate() }) {
                Text(stringResource(Res.string.Dashboard_UpdatePrompt_Action))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(Res.string.Common_Dismiss))
            }
        },
    )
}
