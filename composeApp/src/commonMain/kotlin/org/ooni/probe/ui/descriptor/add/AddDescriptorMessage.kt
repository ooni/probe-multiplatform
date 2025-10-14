package org.ooni.probe.ui.descriptor.add

import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ooniprobe.composeapp.generated.resources.AddDescriptor_Toasts_Installed
import ooniprobe.composeapp.generated.resources.LoadingScreen_Runv2_Failure
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.getString
import org.ooni.probe.LocalSnackbarHostState

@Composable
fun AddDescriptorMessage(
    message: AddDescriptorViewModel.Message?,
    onMessageDisplayed: (AddDescriptorViewModel.Message) -> Unit = { },
) {
    val snackbarHostState = LocalSnackbarHostState.current ?: return
    LaunchedEffect(message) {
        val message = message ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            getString(
                when (message) {
                    AddDescriptorViewModel.Message.AddDescriptorSuccess ->
                        Res.string.AddDescriptor_Toasts_Installed

                    AddDescriptorViewModel.Message.FailedToFetch ->
                        Res.string.LoadingScreen_Runv2_Failure
                },
            ),
        )
        if (result == SnackbarResult.Dismissed) {
            onMessageDisplayed(message)
        }
    }
}
