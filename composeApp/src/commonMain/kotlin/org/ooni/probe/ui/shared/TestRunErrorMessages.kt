package org.ooni.probe.ui.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ooniprobe.composeapp.generated.resources.Modal_Error_CantDownloadURLs
import ooniprobe.composeapp.generated.resources.Modal_Error_NoInternet
import ooniprobe.composeapp.generated.resources.Modal_Error_ProxyUnavailable
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.getString
import org.ooni.probe.LocalSnackbarHostState
import org.ooni.probe.data.models.TestRunError
import kotlin.time.Duration.Companion.seconds

@Composable
fun TestRunErrorMessages(
    errors: List<TestRunError>,
    onErrorDisplayed: (TestRunError) -> Unit,
) {
    val snackbarHostState = LocalSnackbarHostState.current ?: return
    LaunchedEffect(errors) {
        val error = errors.firstOrNull() ?: return@LaunchedEffect
        launch {
            delay(2.seconds) // Shorter snackbar duration
            onErrorDisplayed(error)
        }
        snackbarHostState.showSnackbar(
            getString(
                when (error) {
                    TestRunError.DownloadUrlsFailed -> Res.string.Modal_Error_CantDownloadURLs
                    TestRunError.NoInternet -> Res.string.Modal_Error_NoInternet
                    TestRunError.ProxyUnavailable -> Res.string.Modal_Error_ProxyUnavailable
                },
            ),
        )
    }
}
