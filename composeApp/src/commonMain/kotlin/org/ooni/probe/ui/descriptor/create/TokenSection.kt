package org.ooni.probe.ui.descriptor.create

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Login_CheckEmail
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Login_Token
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Login_Verify
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.ui.descriptor.create.CreateDescriptorViewModel.Event

@Composable
internal fun TokenSection(
    state: CreateDescriptorViewModel.State,
    onEvent: (Event) -> Unit,
) {
    Text(
        stringResource(Res.string.CreateDescriptor_Login_CheckEmail),
        modifier = Modifier.padding(bottom = 16.dp),
    )

    OutlinedTextField(
        value = state.tokenInput,
        onValueChange = { onEvent(Event.TokenChanged(it)) },
        label = { Text(stringResource(Res.string.CreateDescriptor_Login_Token)) },
        isError = state.errorMessage != null,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Uri),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("CreateDescriptor-Token"),
    )
    ErrorText(state.errorMessage)

    Button(
        onClick = { onEvent(Event.VerifyClicked) },
        enabled = !state.isBusy,
        modifier = Modifier
            .padding(top = 16.dp)
            .testTag("CreateDescriptor-Verify"),
    ) {
        Text(stringResource(Res.string.CreateDescriptor_Login_Verify))
    }
}
