package org.ooni.probe.ui.descriptor.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlin.time.Clock
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Discard_Message
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Discard_Title
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_EmailInvalid
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_FieldsRequired
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_InvalidTests
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_InvalidUrls
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_LoginFailed
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_Network
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_NotEditable
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_NotLoggedIn
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_RateLimited
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_Rejected
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_TokenEmpty
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Error_Unexpected
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Title
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Title_Edit
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Modal_OK
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.AuthSession
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.ui.descriptor.create.CreateDescriptorViewModel.ErrorMessage
import org.ooni.probe.ui.descriptor.create.CreateDescriptorViewModel.Event
import org.ooni.probe.ui.descriptor.create.CreateDescriptorViewModel.FormData
import org.ooni.probe.ui.descriptor.create.CreateDescriptorViewModel.State
import org.ooni.probe.ui.shared.NavigationBackButton
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.shared.VerticalScrollbar
import org.ooni.probe.ui.theme.AppTheme

/**
 * Authors a brand new OONI Run v2 link. Each [State] has its own section composable in its own
 * file in this package - this file only dispatches between them and hosts what's shared
 * ([ErrorText], error-message copy, previews).
 */
@Composable
fun CreateDescriptorScreen(
    state: State,
    onEvent: (Event) -> Unit,
) {
    Column(
        Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
    ) {
        TopBar(
            title = {
                Text(
                    stringResource(
                        if ((state as? State.LoggedIn)?.isEditing == true) {
                            Res.string.CreateDescriptor_Title_Edit
                        } else {
                            Res.string.CreateDescriptor_Title
                        },
                    ),
                )
            },
            navigationIcon = {
                NavigationBackButton(onClick = { onEvent(Event.BackClicked) })
            },
        )

        Box(Modifier.weight(1f)) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .imePadding()
                    .padding(
                        bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
                    ).testTag("CreateDescriptor-Content"),
            ) {
                when (state) {
                    State.Loading ->
                        CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))

                    is State.LoggedOut ->
                        LoginSection(state, onEvent)

                    is State.AwaitingToken ->
                        TokenSection(state, onEvent)

                    is State.LoggedIn ->
                        CreateDescriptorForm(state, onEvent)
                }
            }
            VerticalScrollbar(
                state = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }

    if (state.showBackConfirmation) {
        DiscardConfirmDialog(
            onConfirm = { onEvent(Event.BackConfirmed) },
            onDismiss = { onEvent(Event.BackCancelled) },
        )
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.hasChanges || state.showBackConfirmation,
        onBackCompleted = { onEvent(Event.BackClicked) },
    )
}

@Composable
private fun DiscardConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(Res.string.CreateDescriptor_Discard_Title)) },
        text = { Text(stringResource(Res.string.CreateDescriptor_Discard_Message)) },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text(stringResource(Res.string.Modal_OK))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(Res.string.Modal_Cancel))
            }
        },
    )
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
        ErrorMessage.InvalidUrls -> Res.string.CreateDescriptor_Error_InvalidUrls
        ErrorMessage.InvalidNetTests -> Res.string.CreateDescriptor_Error_InvalidTests
        ErrorMessage.NotEditable -> Res.string.CreateDescriptor_Error_NotEditable
        ErrorMessage.Rejected -> Res.string.CreateDescriptor_Error_Rejected
        ErrorMessage.Unexpected -> Res.string.CreateDescriptor_Error_Unexpected
    }

@Preview
@Composable
fun CreateDescriptorScreenLoggedOutPreview() {
    AppTheme {
        CreateDescriptorScreen(
            state = State.LoggedOut(),
            onEvent = {},
        )
    }
}

@Preview
@Composable
fun CreateDescriptorScreenAwaitingTokenPreview() {
    AppTheme {
        CreateDescriptorScreen(
            state = State.AwaitingToken(
                emailAddress = "user@example.org",
                tokenInput = "123456",
            ),
            onEvent = {},
        )
    }
}

@Preview
@Composable
fun CreateDescriptorScreenLoggedInPreview() {
    AppTheme {
        CreateDescriptorScreen(
            state = State.LoggedIn(
                session = AuthSession(
                    sessionToken = "",
                    emailAddress = "user@example.org",
                    role = "",
                    loginTime = Clock.System.now(),
                ),
            ),
            onEvent = {},
        )
    }
}

@Preview
@Composable
fun CreateDescriptorScreenEditingPreview() {
    AppTheme {
        CreateDescriptorScreen(
            state = State.LoggedIn(
                session = AuthSession(
                    sessionToken = "",
                    emailAddress = "user@example.org",
                    role = "",
                    loginTime = Clock.System.now(),
                ),
                form = FormData(
                    name = "Existing Test List",
                    shortDescription = "Short description for existing descriptor",
                    description = "Detailed description of the descriptor being edited.",
                ),
                editingDescriptorId = Descriptor.Id("123"),
            ),
            onEvent = {},
        )
    }
}

@Preview
@Composable
fun CreateDescriptorScreenEditingDiscardModalPreview() {
    AppTheme {
        CreateDescriptorScreen(
            state = State.LoggedIn(
                session = AuthSession(
                    sessionToken = "",
                    emailAddress = "user@example.org",
                    role = "",
                    loginTime = Clock.System.now(),
                ),
                form = FormData(
                    name = "Existing Test List",
                    shortDescription = "Short description for existing descriptor",
                    description = "Detailed description of the descriptor being edited.",
                ),
                editingDescriptorId = Descriptor.Id("123"),
                showBackConfirmation = true,
            ),
            onEvent = {},
        )
    }
}
