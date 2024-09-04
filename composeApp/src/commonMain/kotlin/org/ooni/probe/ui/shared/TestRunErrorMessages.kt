package org.ooni.probe.ui.shared

import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import ooniprobe.composeapp.generated.resources.Modal_Error_CantDownloadURLs
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.LocalSnackbarHostState
import org.ooni.probe.data.models.TestRunError

@Composable
fun TestRunErrorMessages(
    errors: List<TestRunError>,
    onErrorDisplayed: (TestRunError) -> Unit,
) {
    val snackbarHostState = LocalSnackbarHostState.current ?: return
    val errorMessage = when (errors.firstOrNull()) {
        TestRunError.DownloadUrlsFailed -> stringResource(Res.string.Modal_Error_CantDownloadURLs)
        null -> ""
    }
    LaunchedEffect(errors) {
        val error = errors.firstOrNull() ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(errorMessage)
        if (result == SnackbarResult.Dismissed) {
            onErrorDisplayed(error)
        }
    }
}
