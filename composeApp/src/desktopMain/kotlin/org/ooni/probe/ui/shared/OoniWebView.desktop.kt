package org.ooni.probe.ui.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.winterreisender.webviewko.WebviewKoCompose.Webview

@Composable
actual fun OoniWebView(
    controller: OoniWebViewController,
    modifier: Modifier,
    allowedDomains: List<String>,
) {
    Webview(
        "https://google.com",
        debug = true,
        modifier = modifier
    )
}
