package org.ooni.probe.ui.shared

import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ooniprobe.composeapp.generated.resources.AddDescriptor_Toasts_Installed
import ooniprobe.composeapp.generated.resources.LoadingScreen_Runv2_Canceled
import ooniprobe.composeapp.generated.resources.LoadingScreen_Runv2_Failure
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.LocalSnackbarHostState
import org.ooni.probe.data.models.SnackBarMessage

@Composable
fun NotificationMessages(
    message: List<SnackBarMessage>,
    onMessageDisplayed: (SnackBarMessage) -> Unit = { },
) {
    val snackbarHostState = LocalSnackbarHostState.current ?: return
    val errorMessage = when (message.firstOrNull()) {
        SnackBarMessage.AddDescriptorSuccess -> stringResource(Res.string.AddDescriptor_Toasts_Installed)
        SnackBarMessage.AddDescriptorFailed -> stringResource(Res.string.LoadingScreen_Runv2_Failure)
        SnackBarMessage.AddDescriptorCancel -> stringResource(Res.string.LoadingScreen_Runv2_Canceled)
        else -> ""
    }
    LaunchedEffect(message) {
        val error = message.firstOrNull() ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(errorMessage)
        if (result == SnackbarResult.Dismissed) {
            onMessageDisplayed(error)
        }
    }
}
