package org.ooni.probe.ui.shared

import androidx.compose.runtime.Composable
import com.multiplatform.webview.request.RequestInterceptor
import com.multiplatform.webview.request.WebRequest
import com.multiplatform.webview.request.WebRequestInterceptResult
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

@Composable
fun MarkdownViewer(
    markdown: String,
    onUrlClicked: (String) -> Unit,
) {
    val flavour = CommonMarkFlavourDescriptor()
    val html = HtmlGenerator(
        markdown,
        MarkdownParser(flavour).buildMarkdownTreeFromString(markdown),
        flavour,
    ).generateHtml()
    val webViewState = rememberWebViewStateWithHTMLData(
        data = html,
    )
    val navigator =
        rememberWebViewNavigator(
            requestInterceptor =
                object : RequestInterceptor {
                    override fun onInterceptUrlRequest(
                        request: WebRequest,
                        navigator: WebViewNavigator,
                    ): WebRequestInterceptResult {
                        onUrlClicked(request.url)
                        return WebRequestInterceptResult.Reject
                    }
                },
        )
    WebView(state = webViewState, navigator = navigator)
}
