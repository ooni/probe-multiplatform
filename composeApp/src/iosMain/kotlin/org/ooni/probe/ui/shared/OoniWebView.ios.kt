package org.ooni.probe.ui.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger
import com.multiplatform.webview.request.RequestInterceptor
import com.multiplatform.webview.request.WebRequest
import com.multiplatform.webview.request.WebRequestInterceptResult
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import io.ktor.http.parseUrl
import platform.UIKit.UIColor
import platform.WebKit.WKContentRuleListStore

@Composable
actual fun OoniWebView(
    controller: OoniWebViewController,
    modifier: Modifier,
    allowedDomains: List<String>,
    onDisallowedUrl: (String) -> Unit, // TODO
) {
    val event = controller.rememberNextEvent()
    val state = rememberWebViewState("about:blank")
    val navigator = rememberWebViewNavigator(
        requestInterceptor = object : RequestInterceptor {
            override fun onInterceptUrlRequest(
                request: WebRequest,
                navigator: WebViewNavigator,
            ): WebRequestInterceptResult {
                val url = parseUrl(request.url) ?: return WebRequestInterceptResult.Allow

                val isAllowed = allowedDomains.any { domain ->
                    url.host.matches(Regex("^(.*\\.)?$domain$"))
                }
                return if (isAllowed) {
                    WebRequestInterceptResult.Allow
                } else {
                    onDisallowedUrl(request.url)
                    WebRequestInterceptResult.Reject
                }
            }
        },
    )

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
        onCreated = {
            it.backgroundColor = UIColor.whiteColor
            val blockRules = """
            [{
                "trigger": {
                    "url-filter": ".*",
                    "unless-domain": [${allowedDomains.joinToString(",") { "\"*$it\"" }}]
                },
                "action": {
                    "type": "block"
                }
            }]
            """

            WKContentRuleListStore.defaultStore()?.compileContentRuleListForIdentifier(
                identifier = "ContentBlockingRules",
                encodedContentRuleList = blockRules,
            ) { contentRuleList, error ->
                if (error != null || contentRuleList == null) {
                    Logger.w("Error compiling WKWebView content block rules: ${error?.localizedDescription()}")
                    return@compileContentRuleListForIdentifier
                }
                state.nativeWebView.backgroundColor = UIColor.whiteColor
                state.nativeWebView.configuration.userContentController.addContentRuleList(
                    contentRuleList,
                )
            }
        },
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
