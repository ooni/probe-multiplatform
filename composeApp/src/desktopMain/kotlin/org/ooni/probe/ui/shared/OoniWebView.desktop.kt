package org.ooni.probe.ui.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.web.WebView
import java.net.URI
import java.net.URL
import kotlin.io.encoding.Base64

@Composable
actual fun OoniWebView(
    controller: OoniWebViewController,
    modifier: Modifier,
    allowedDomains: List<String>,
) {
    val event = controller.rememberNextEvent()

    SwingPanel(
        factory = {
            controller.state = OoniWebViewController.State.Initializing

            JFXPanel().apply {
                Platform.setImplicitExit(false) // Otherwise, webView will not show the second time
                Platform.runLater {
                    val webView = WebView().apply {
                        isVisible = true
                        @Suppress("SetJavaScriptEnabled")
                        engine.isJavaScriptEnabled = true

                        // Set up load listeners
                        engine.loadWorker.stateProperty().addListener { _, _, newValue ->
                            when (newValue) {
                                Worker.State.SCHEDULED -> {
                                    controller.state = OoniWebViewController.State.Loading(0f)
                                }

                                Worker.State.RUNNING -> {
                                    val progress = engine.loadWorker.progress
                                    controller.state =
                                        OoniWebViewController.State.Loading(progress.toFloat())
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
                                val host = URI.create(newLocation).host
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

                        val css = """
                            body {
                                -ms-overflow-style: none;  /* Internet Explorer 10+ */
                                scrollbar-width: none;  /* Firefox */
                            }
                            body::-webkit-scrollbar {
                                display: none;  /* Safari and Chrome */
                            }
                        """.trimIndent()
                        val cssData = Base64.encode(css.encodeToByteArray())
                        engine.userStyleSheetLocation =
                            "data:text/css;charset=utf-8;base64,$cssData"
                    }

                    val root = StackPane()
                    root.children.add(webView)
                    this.scene = Scene(root)
                }
            }
        },
        modifier = modifier,
        update = { jfxPanel ->
            Platform.runLater {
                val root = jfxPanel.scene?.root as? StackPane
                val webView = (root?.children?.get(0) as? WebView) ?: return@runLater
                when (event) {
                    is OoniWebViewController.Event.Load -> {
                        val headers = event.additionalHttpHeaders.entries.joinToString {
                            "\n${it.key}: it.value"
                        }
                        // Hack to send HTTP headers by taking advantage of userAgent
                        webView.engine.userAgent = "ooni$headers"
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
                event?.let(controller::onEventHandled)
            }
        },
    )
}
