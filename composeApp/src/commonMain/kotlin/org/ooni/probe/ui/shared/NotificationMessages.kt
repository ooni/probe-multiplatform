package org.ooni.probe.ui.shared

import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ooniprobe.composeapp.generated.resources.AddDescriptor_Toasts_Installed
import ooniprobe.composeapp.generated.resources.LoadingScreen_Runv2_Canceled
import ooniprobe.composeapp.generated.resources.LoadingScreen_Runv2_Failure
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.getString
import org.ooni.probe.LocalSnackbarHostState
import org.ooni.probe.ui.descriptor.add.AddDescriptorViewModel

@Composable
fun NotificationMessages(
    message: List<AddDescriptorViewModel.SnackBarMessage>,
    onMessageDisplayed: (AddDescriptorViewModel.SnackBarMessage) -> Unit = { },
) {
    val snackbarHostState = LocalSnackbarHostState.current ?: return
    LaunchedEffect(message) {
        val errorMessage = when (message.firstOrNull()) {
            AddDescriptorViewModel.SnackBarMessage.AddDescriptorSuccess -> getString(Res.string.AddDescriptor_Toasts_Installed)
            AddDescriptorViewModel.SnackBarMessage.AddDescriptorFailed -> getString(Res.string.LoadingScreen_Runv2_Failure)
            AddDescriptorViewModel.SnackBarMessage.AddDescriptorCancel -> getString(Res.string.LoadingScreen_Runv2_Canceled)
            else -> ""
        }
        val error = message.firstOrNull() ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(errorMessage)
        if (result == SnackbarResult.Dismissed) {
            onMessageDisplayed(error)
        }
    }
}
