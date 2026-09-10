package org.ooni.probe.ui.descriptor.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_EmailInvalid
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_FieldsRequired
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_InvalidOptions
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_InvalidTests
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_LoginFailed
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_Network
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_NotLoggedIn
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_RateLimited
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_Rejected
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_TokenEmpty
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_Unexpected
import ooniprobe.composeapp.generated.resources.CreateDescriptor_LogOut
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Title
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.ui.descriptor.create.CreateDescriptorViewModel.ErrorMessage
import org.ooni.probe.ui.descriptor.create.CreateDescriptorViewModel.Event
import org.ooni.probe.ui.descriptor.create.CreateDescriptorViewModel.Phase
import org.ooni.probe.ui.shared.LocalClipboardActions
import org.ooni.probe.ui.shared.NavigationBackButton
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.shared.VerticalScrollbar
import org.ooni.probe.ui.theme.AppTheme

/**
 * Authors a brand new OONI Run v2 link. Each [Phase] has its own section composable in its own
 * file in this package - this file only dispatches between them and hosts what's shared
 * ([ErrorText], error-message copy, previews).
 */
@Composable
fun CreateDescriptorScreen(
    state: CreateDescriptorViewModel.State,
    onEvent: (Event) -> Unit,
) {
    val clipboardActions = LocalClipboardActions.current

    Column(
        Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
    ) {
        TopBar(
            title = { Text(stringResource(Res.string.CreateDescriptor_Title)) },
            navigationIcon = {
                NavigationBackButton(onClick = { onEvent(Event.BackClicked) })
            },
            actions = {
                if (state.session != null && state.phase != Phase.Success) {
                    TextButton(
                        onClick = { onEvent(Event.LogOutClicked) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        Text(stringResource(Res.string.CreateDescriptor_LogOut))
                    }
                }
            },
        )

        Box(Modifier.weight(1f)) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .imePadding()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 36.dp)
                    .testTag("CreateDescriptor-Content"),
            ) {
                when (state.phase) {
                    Phase.Loading ->
                        CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))

                    Phase.LoggedOut -> LoginSection(state, onEvent)
                    Phase.AwaitingToken -> TokenSection(state, onEvent)
                    Phase.LoggedIn, Phase.Submitting -> CreateForm(state, onEvent)
                    Phase.Success -> SuccessSection(state, onEvent)
                }
            }
            VerticalScrollbar(
                state = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }

    LaunchedEffect(state.copyToClipboard) {
        state.copyToClipboard?.let { text ->
            clipboardActions?.copyToClipboard(text)
            onEvent(Event.CopyShown)
        }
    }
}

@Composable
internal fun ErrorText(errorMessage: ErrorMessage?) {
    errorMessage ?: return
    Text(
        text = stringResource(errorMessage.toStringResource()),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .padding(top = 4.dp)
            .testTag("CreateDescriptor-Error"),
    )
}

private fun ErrorMessage.toStringResource(): StringResource =
    when (this) {
        ErrorMessage.EmailInvalid -> Res.string.CreateDescriptor_Error_EmailInvalid
        ErrorMessage.TokenEmpty -> Res.string.CreateDescriptor_Error_TokenEmpty
        ErrorMessage.LoginFailed -> Res.string.CreateDescriptor_Error_LoginFailed
        ErrorMessage.NotLoggedIn -> Res.string.CreateDescriptor_Error_NotLoggedIn
        ErrorMessage.Network -> Res.string.CreateDescriptor_Error_Network
        ErrorMessage.RateLimited -> Res.string.CreateDescriptor_Error_RateLimited
        ErrorMessage.FieldsRequired -> Res.string.CreateDescriptor_Error_FieldsRequired
        ErrorMessage.InvalidOptions -> Res.string.CreateDescriptor_Error_InvalidOptions
        ErrorMessage.InvalidNetTests -> Res.string.CreateDescriptor_Error_InvalidTests
        ErrorMessage.Rejected -> Res.string.CreateDescriptor_Error_Rejected
        ErrorMessage.Unexpected -> Res.string.CreateDescriptor_Error_Unexpected
    }

@Preview
@Composable
fun CreateDescriptorScreenLoggedOutPreview() {
    AppTheme {
        CreateDescriptorScreen(
            state = CreateDescriptorViewModel.State(phase = Phase.LoggedOut),
            onEvent = {},
        )
    }
}

@Preview
@Composable
fun CreateDescriptorScreenSuccessPreview() {
    AppTheme {
        CreateDescriptorScreen(
            state = CreateDescriptorViewModel.State(
                phase = Phase.Success,
                runLink = "https://run.ooni.org/v2/1234",
                deepLink = "ooni://runv2/1234",
            ),
            onEvent = {},
        )
    }
}
