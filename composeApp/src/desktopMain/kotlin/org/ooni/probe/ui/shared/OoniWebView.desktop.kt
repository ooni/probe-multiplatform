package org.ooni.probe.ui.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.embed.swing.JFXPanel
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.web.WebView

@Composable
actual fun OoniWebView(
    controller: OoniWebViewController,
    modifier: Modifier,
    allowedDomains: List<String>,
) {
    val onCreated = {}
    val onDispose = {}
    val currentOnDispose by rememberUpdatedState(onDispose)

    DisposableEffect(Unit) {
        onDispose {
            currentOnDispose()
        }
    }

    SwingPanel(
        factory = {
            JFXPanel().apply {
                Platform.runLater {
                    val webView = WebView().apply {
                        isVisible = true
                        // engine.addLoadListener(state, navigator)
                        engine.isJavaScriptEnabled = true
                    }
                    val root = StackPane()
                    root.children.add(webView)
                    this.scene = Scene(root)
                    // state.webView = DesktopWebView(webView)
                    onCreated()
                    webView.engine.load("https://ooni.org")
                }
            }
        },
        modifier = modifier,
    )
}
