package org.ooni.probe.ui.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState

@Composable
actual fun OoniWebView(
    controller: OoniWebViewController,
    modifier: Modifier,
    allowedDomains: List<String>,
) {
    val event = controller.rememberNextEvent()
    val state = rememberWebViewState("about:blank")
    val navigator = rememberWebViewNavigator()

    LaunchedEffect(state.loadingState, state.errorsForCurrentRequest) {
        val errors = state.errorsForCurrentRequest
        controller.state = when (val loadingState = state.loadingState) {
            LoadingState.Initializing -> OoniWebViewController.State.Initializing
            is LoadingState.Loading -> OoniWebViewController.State.Loading(loadingState.progress)
            LoadingState.Finished -> if (errors.any { it.isFromMainFrame }) {
                OoniWebViewController.State.Failure
            } else {
                OoniWebViewController.State.Successful
            }
        }
    }

    WebView(
        state = state,
        navigator = navigator,
        captureBackPresses = true,
        modifier = modifier,
        factory = null,
    )

    LaunchedEffect(event) {
        when (event) {
            is OoniWebViewController.Event.Load ->
                navigator.loadUrl(event.url, event.additionalHttpHeaders)

            OoniWebViewController.Event.Reload ->
                navigator.reload()

            OoniWebViewController.Event.Back -> {
                // Navigator is handling back for us
            }

            null -> Unit
        }
        event?.let(controller::onEventHandled)
    }
}
