package org.ooni.probe.ui.shared

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import ooniprobe.composeapp.generated.resources.Modal_Autorun_BatteryOptimization_Reminder
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Modal_OK
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource

@Composable
fun IgnoreBatteryOptimizationDialog(
    onAccepted: () -> Unit,
    onDismissed: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissed,
        text = { Text(stringResource(Res.string.Modal_Autorun_BatteryOptimization_Reminder)) },
        confirmButton = {
            TextButton(onClick = onAccepted) {
                Text(stringResource(Res.string.Modal_OK))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissed) {
                Text(stringResource(Res.string.Modal_Cancel))
            }
        },
    )
}
