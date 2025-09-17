package org.ooni.probe.ui.shared

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ooniprobe.composeapp.generated.resources.Modal_AlwaysRun
import ooniprobe.composeapp.generated.resources.Modal_DisableVPN
import ooniprobe.composeapp.generated.resources.Modal_DisableVPN_Message
import ooniprobe.composeapp.generated.resources.Modal_DisableVPN_Title
import ooniprobe.composeapp.generated.resources.Modal_OK
import ooniprobe.composeapp.generated.resources.Modal_RunAnyway
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_DisableVpnInstructions
import org.jetbrains.compose.resources.stringResource

@Composable
fun VpnWarningDialog(
    onDismiss: () -> Unit,
    onRunAnyway: () -> Unit,
    onRunAlways: () -> Unit,
    onDisableVpn: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(Res.string.Modal_DisableVPN_Title)) },
        text = { Text(stringResource(Res.string.Modal_DisableVPN_Message)) },
        confirmButton = {
            Row {
                TextButton(onClick = { onRunAlways() }, Modifier.weight(1f)) {
                    Text(stringResource(Res.string.Modal_AlwaysRun))
                }
                TextButton(onClick = { onRunAnyway() }, Modifier.weight(1f)) {
                    Text(stringResource(Res.string.Modal_RunAnyway))
                }
                TextButton(onClick = { onDisableVpn() }, Modifier.weight(1f)) {
                    Text(stringResource(Res.string.Modal_DisableVPN))
                }
            }
        },
    )
}

@Composable
fun DisableVpnInstructionsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(Res.string.Modal_DisableVPN)) },
        text = { Text(stringResource(Res.string.Settings_DisableVpnInstructions)) },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(Res.string.Modal_OK))
            }
        },
    )
}
