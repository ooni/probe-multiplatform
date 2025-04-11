package org.ooni.probe.ui.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.web.WebView
import java.net.URL

@Composable
actual fun OoniWebView(
    controller: OoniWebViewController,
    modifier: Modifier,
    allowedDomains: List<String>,
) {
    val onCreated = {}
    val onDispose = {}
    val currentOnDispose by rememberUpdatedState(onDispose)
    val event = controller.rememberNextEvent()

    SwingPanel(
        factory = {
            JFXPanel().apply {
                Platform.runLater {
                    val webView = WebView().apply {
                        isVisible = true
                        engine.isJavaScriptEnabled = true

                        // Set up load listeners
                        engine.loadWorker.stateProperty().addListener { _, _, newValue ->
                            when (newValue) {
                                Worker.State.SCHEDULED -> {
                                    controller.state = OoniWebViewController.State.Loading(0f)
                                }
                                Worker.State.RUNNING -> {
                                    val progress = engine.loadWorker.progress
                                    controller.state = OoniWebViewController.State.Loading(progress.toFloat())
                                }
                                Worker.State.SUCCEEDED -> {
                                    controller.state = OoniWebViewController.State.Successful
                                    controller.canGoBack = engine.history.currentIndex > 0
                                }
                                Worker.State.FAILED -> {
                                    controller.state = OoniWebViewController.State.Failure
                                    controller.canGoBack = engine.history.currentIndex > 0
                                }
                                else -> {}
                            }
                        }

                        // Domain restriction
                        engine.locationProperty().addListener { _, _, newLocation ->
                            try {
                                val host = URL(newLocation).host
                                val allowed = allowedDomains.any { domain ->
                                    host.matches(Regex("^(.*\\.)?$domain$"))
                                }

                                if (!allowed) {
                                    engine.load("about:blank")
                                }
                            } catch (e: Exception) {
                                // Invalid URL, ignore
                            }
                            controller.canGoBack = engine.history.currentIndex > 0
                        }
                    }

                    val root = StackPane()
                    root.children.add(webView)
                    this.scene = Scene(root)
                    onCreated()
                }
            }
        },
        modifier = modifier,
        update = { jfxPanel ->
            Platform.runLater {
                val scene = jfxPanel.scene
                val root = scene?.root as? StackPane
                val webView = root?.children?.get(0) as? WebView
                webView?.let {
                    when (event) {
                        is OoniWebViewController.Event.Load -> {
                            webView.engine.load(event.url)
                        }
                        OoniWebViewController.Event.Reload -> {
                            webView.engine.reload()
                        }
                        OoniWebViewController.Event.Back -> {
                            if (webView.engine.history.currentIndex > 0) {
                                webView.engine.history.go(-1)
                            }
                        }
                        null -> Unit
                    }
                }
            }
            event?.let(controller::onEventHandled)
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            currentOnDispose()
        }
    }
}
