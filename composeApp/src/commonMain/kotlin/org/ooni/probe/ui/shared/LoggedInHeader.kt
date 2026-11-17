package org.ooni.probe.ui.shared

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.CreateDescriptor_LogOut
import ooniprobe.composeapp.generated.resources.CreateDescriptor_LoggedInAs
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoggedInHeader(
    emailAddress: String,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 8.dp),
        ) {
            Text(
                stringResource(Res.string.CreateDescriptor_LoggedInAs, emailAddress),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onLogOut,
                modifier = Modifier.testTag("LoggedInHeader-LogOut"),
            ) {
                Text(
                    stringResource(Res.string.CreateDescriptor_LogOut),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
