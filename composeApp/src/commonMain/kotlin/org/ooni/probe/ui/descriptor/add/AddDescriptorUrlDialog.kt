package org.ooni.probe.ui.descriptor.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.AddDescriptor_EnterURL
import ooniprobe.composeapp.generated.resources.AddDescriptor_Title
import ooniprobe.composeapp.generated.resources.AddDescriptor_URLInvalid
import ooniprobe.composeapp.generated.resources.Common_Next
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AddDescriptorUrlDialog(
    state: AddDescriptorUrlViewModel.State,
    onEvent: (AddDescriptorUrlViewModel.Event) -> Unit,
) {
    Surface(shape = MaterialTheme.shapes.medium) {
        Column(
            Modifier.padding(all = 16.dp).fillMaxWidth(),
        ) {
            Text(
                stringResource(Res.string.AddDescriptor_Title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
            Text(
                stringResource(Res.string.AddDescriptor_EnterURL),
                modifier = Modifier.padding(bottom = 16.dp),
            )

            OutlinedTextField(
                value = state.input,
                onValueChange = { onEvent(AddDescriptorUrlViewModel.Event.InputChanged(it)) },
                placeholder = { Text(AddDescriptorUrlViewModel.RUN_LINK_PREFIX + "…") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Uri,
                ),
                isError = state.isInvalid,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.isInvalid) {
                Text(
                    text = stringResource(Res.string.AddDescriptor_URLInvalid),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                TextButton(onClick = { onEvent(AddDescriptorUrlViewModel.Event.CloseClicked) }) {
                    Text(stringResource(Res.string.Modal_Cancel))
                }
                Button(
                    onClick = { onEvent(AddDescriptorUrlViewModel.Event.NextClicked) },
                    enabled = !state.isInvalid,
                ) {
                    Text(stringResource(Res.string.Common_Next))
                }
            }
        }
    }
}

@Composable
@Preview
fun AddDescriptorUrlDialogPreview() {
    AddDescriptorUrlDialog(
        state = AddDescriptorUrlViewModel.State(),
        onEvent = {},
    )
}

@Composable
@Preview
fun AddDescriptorUrlDialogInvalidPreview() {
    AddDescriptorUrlDialog(
        state = AddDescriptorUrlViewModel.State(input = "invalid", isInvalid = true),
        onEvent = {},
    )
}
