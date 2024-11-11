package org.ooni.probe.ui.shared

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun OoniWebView(
    controller: OoniWebViewController,
    modifier: Modifier,
    allowedDomains: List<String>,
) {
    fun isRequestAllowed(request: WebResourceRequest) =
        allowedDomains.any { domain ->
            request.url.host
                ?.matches(Regex("^(.*\\.)?$domain$")) == true
        }

    val event = controller.rememberNextEvent()

    BackHandler(controller.canGoBack) {
        controller.goBack()
    }

    AndroidView(
        factory = {
            val webView = WebView(it)
            webView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            // Avoid covering other components
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            @SuppressLint("SetJavaScriptEnabled")
            webView.settings.javaScriptEnabled = true
            webView.webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? {
                    return if (isRequestAllowed(request)) {
                        super.shouldInterceptRequest(view, request)
                    } else {
                        WebResourceResponse(
                            "",
                            "",
                            401,
                            "Unauthorized",
                            emptyMap(),
                            "".byteInputStream(),
                        )
                    }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ) = !isRequestAllowed(request)

                override fun onPageStarted(
                    view: WebView,
                    url: String?,
                    favicon: Bitmap?,
                ) {
                    controller.state = OoniWebViewController.State.Loading(0f)
                    controller.canGoBack = view.canGoBack()
                }

                override fun onPageFinished(
                    view: WebView,
                    url: String?,
                ) {
                    controller.state = OoniWebViewController.State.Finished
                    controller.canGoBack = view.canGoBack()
                }

                override fun onLoadResource(
                    view: WebView,
                    url: String?,
                ) {
                    controller.canGoBack = view.canGoBack()
                }
            }

            webView.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(
                    view: WebView,
                    newProgress: Int,
                ) {
                    controller.state = OoniWebViewController.State.Loading(newProgress / 100f)
                }
            }
            webView
        },
        modifier = modifier,
        update = { webView ->
            when (event) {
                is OoniWebViewController.Event.Load ->
                    webView.loadUrl(event.url, event.additionalHttpHeaders)

                OoniWebViewController.Event.Reload ->
                    webView.reload()

                OoniWebViewController.Event.Back -> {
                    webView.goBack()
                }

                null -> Unit
            }
            event?.let(controller::onEventHandled)
            controller.canGoBack = webView.canGoBack()
        },
    )
}
