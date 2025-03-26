package org.ooni.probe.ui.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun OoniWebView(
    controller: OoniWebViewController,
    modifier: Modifier,
    allowedDomains: List<String>,
) {
}
