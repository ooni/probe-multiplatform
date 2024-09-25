package org.ooni.probe.ui.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import ooniprobe.composeapp.generated.resources.Modal_Error_CantDownloadURLs
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.LocalSnackbarHostState
import org.ooni.probe.data.models.TestRunError
import kotlin.time.Duration.Companion.seconds

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
        snackbarHostState.showSnackbar(errorMessage)
        delay(0.5.seconds) // No need to wait for the snackbar to be fully dismissed
        onErrorDisplayed(error)
    }
}
