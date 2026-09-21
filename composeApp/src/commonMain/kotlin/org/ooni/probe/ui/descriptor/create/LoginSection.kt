package org.ooni.probe.ui.descriptor.create

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Login_Email
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Login_Explanation
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Login_SendLink
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.ui.descriptor.create.CreateDescriptorViewModel.ErrorMessage
import org.ooni.probe.ui.descriptor.create.CreateDescriptorViewModel.Event
import org.ooni.probe.ui.descriptor.create.CreateDescriptorViewModel.State

@Composable
internal fun LoginSection(
    state: State.LoggedOut,
    onEvent: (Event) -> Unit,
) {
    Column(
        modifier = Modifier.padding(all = 16.dp),
    ) {
        Text(
            stringResource(Res.string.CreateDescriptor_Login_Explanation),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 16.dp),
        )

        OutlinedTextField(
            value = state.emailAddress,
            onValueChange = { onEvent(Event.EmailChanged(it)) },
            label = { Text(stringResource(Res.string.CreateDescriptor_Login_Email)) },
            singleLine = true,
            isError = state.errorMessage == ErrorMessage.EmailInvalid,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("CreateDescriptor-Email"),
        )
        ErrorText(state.errorMessage)

        Button(
            onClick = { onEvent(Event.SendLoginLinkClicked) },
            enabled = !state.isBusy,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .testTag("CreateDescriptor-SendLoginLink"),
        ) {
            Text(
                text = stringResource(Res.string.CreateDescriptor_Login_SendLink),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
