package org.ooni.probe.screenshots

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Box
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Scene
import javafx.scene.SnapshotParameters
import javafx.scene.image.WritableImage
import javafx.scene.layout.StackPane
import javafx.scene.web.WebView
import javafx.stage.Stage
import javafx.stage.StageStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.ooni.probe.ui.shared.OoniWebViewController
import org.ooni.probe.ui.shared.ooniWebViewOverride
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

/**
 * JavaFX [WebView] is a native peer when embedded via `SwingPanel` — under
 * `runDesktopComposeUiTest` the offscreen Skiko renderer drops it from the captured PNG. By
 * taking the snapshot ourselves and showing it through an [ImageBitmap], the capture pipeline
 * stays unchanged. The snapshot work happens in the test body via [preloadExplorerSnapshot] (not
 * inside a `LaunchedEffect`), so the test recomposer never sits idle waiting on the network.
 */
internal fun installScreenshotWebViewOverride() {
    ooniWebViewOverride = { controller, modifier, _, _ ->
        ScreenshotWebView(controller, modifier)
    }
}

internal const val TAG = "ExplorerWebViewSnapshot"

private val preloadedSnapshots = ConcurrentHashMap<String, ImageBitmap>()
private val jfxStarted = AtomicBoolean(false)

/**
 * Loads [url] in a real JavaFX [WebView], polls the DOM until [expectedDomText] is present
 * (when provided), snapshots the WebView at [widthPx] × [heightPx], and caches the result so the
 * Compose override can show it as soon as [MeasurementScreen] requests it.
 */
internal suspend fun preloadExplorerSnapshot(
    url: String,
    widthPx: Int,
    heightPx: Int,
    expectedDomText: String?,
) {
    if (preloadedSnapshots.containsKey(url)) return
    val bitmap = captureExplorerSnapshot(url, widthPx, heightPx, expectedDomText)
    preloadedSnapshots[url] = bitmap
}

@Composable
private fun ScreenshotWebView(
    controller: OoniWebViewController,
    modifier: Modifier,
) {
    val event = controller.rememberNextEvent()
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(event) {
        val loadEvent = event as? OoniWebViewController.Event.Load ?: return@LaunchedEffect
        controller.onEventHandled(loadEvent)
        // Remove from the cache so the ImageBitmap can be GC'd as soon as the test moves on to
        // the next locale. Keeping ~30 ImageBitmaps live alongside the JavaFX WebView state has
        // been enough to crash the gradle test worker JVM.
        val cached = preloadedSnapshots.remove(loadEvent.url)
        bitmap = cached
        controller.state = if (cached != null) {
            OoniWebViewController.State.Successful
        } else {
            OoniWebViewController.State.Failure
        }
    }

    Box(modifier = modifier.background(Color.White)) {
        bitmap?.let { rendered ->
            Image(
                bitmap = rendered,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize().testTag(TAG),
            )
        }
    }
}

private suspend fun captureExplorerSnapshot(
    url: String,
    widthPx: Int,
    heightPx: Int,
    expectedDomText: String?,
): ImageBitmap =
    withContext(Dispatchers.Default) {
        ensureJavaFxStarted()
        val (webView, stage) = createWebViewOnFxThread(widthPx, heightPx)
        try {
            awaitPageLoaded(webView, url)
            waitForDomText(webView, expectedDomText)
            // Brief settle so late-arriving fonts/icons paint before snapshot.
            delay(1.seconds)
            snapshotOnFxThread(webView, widthPx, heightPx)
        } finally {
            closeStageOnFxThread(stage, webView)
        }
    }

private fun ensureJavaFxStarted() {
    if (jfxStarted.compareAndSet(false, true)) {
        // JFXPanel constructor bootstraps the JavaFX toolkit on the FX thread. This mirrors the
        // production OoniWebView.desktop.kt bootstrap path.
        JFXPanel()
        Platform.setImplicitExit(false)
    }
}

private suspend fun createWebViewOnFxThread(
    widthPx: Int,
    heightPx: Int,
): Pair<WebView, Stage> =
    suspendCancellableCoroutine { cont ->
        Platform.runLater {
            val webView = WebView().apply {
                prefWidth = widthPx.toDouble()
                prefHeight = heightPx.toDouble()
                @Suppress("SetJavaScriptEnabled")
                engine.isJavaScriptEnabled = true
            }
            // JavaFX WebView only triggers layout / paint pulses when its Scene is in a shown Stage.
            // Use a TRANSPARENT, opacity-0, undecorated stage at the full snapshot size, positioned
            // far off-screen — visible enough for the render loop, invisible to the user.
            val stage = Stage(StageStyle.UNDECORATED).apply {
                scene = Scene(StackPane(webView), widthPx.toDouble(), heightPx.toDouble())
                x = -32000.0
                y = -32000.0
                isAlwaysOnTop = false
                show()
            }
            if (cont.isActive) cont.resume(webView to stage)
        }
    }

private suspend fun closeStageOnFxThread(
    stage: Stage,
    webView: WebView,
) {
    suspendCancellableCoroutine<Unit> { cont ->
        Platform.runLater {
            runCatching {
                // Aggressively drop references the WebView's native peer holds onto. Without
                // this, 30 locales × 2 measurement tests accumulate enough native state to
                // crash the gradle test worker.
                webView.engine.load("about:blank")
                webView.engine.history.entries
                    .clear()
                stage.scene = null
                stage.close()
            }
            if (cont.isActive) cont.resume(Unit)
        }
    }
}

private suspend fun awaitPageLoaded(
    webView: WebView,
    url: String,
) {
    withTimeout(45.seconds) {
        suspendCancellableCoroutine<Unit> { cont ->
            Platform.runLater {
                val listener = object : javafx.beans.value.ChangeListener<Worker.State> {
                    override fun changed(
                        obs: javafx.beans.value.ObservableValue<out Worker.State>?,
                        old: Worker.State?,
                        new: Worker.State?,
                    ) {
                        when (new) {
                            Worker.State.SUCCEEDED -> {
                                webView.engine.loadWorker
                                    .stateProperty()
                                    .removeListener(this)
                                if (cont.isActive) cont.resume(Unit)
                            }
                            Worker.State.FAILED, Worker.State.CANCELLED -> {
                                webView.engine.loadWorker
                                    .stateProperty()
                                    .removeListener(this)
                                if (cont.isActive) cont.cancel(IllegalStateException("Page load $new for $url"))
                            }
                            else -> Unit
                        }
                    }
                }
                webView.engine.loadWorker
                    .stateProperty()
                    .addListener(listener)
                webView.engine.load(url)
            }
        }
    }
}

private suspend fun waitForDomText(
    webView: WebView,
    expected: String?,
) {
    if (expected.isNullOrBlank()) {
        delay(4.seconds)
        return
    }
    val found = runCatching {
        withTimeout(20.seconds) {
            while (true) {
                val visible = evaluateDomText(webView)
                if (visible != null && visible.contains(expected, ignoreCase = true)) return@withTimeout true
                delay(500)
            }
            @Suppress("UNREACHABLE_CODE")
            false
        }
    }.getOrDefault(false)
    if (!found) {
        // Best-effort settle window before snapshot.
        delay(2.seconds)
    }
}

private suspend fun evaluateDomText(webView: WebView): String? =
    suspendCancellableCoroutine { cont ->
        Platform.runLater {
            val result = runCatching {
                webView.engine
                    .executeScript(
                        "(document && document.body) ? document.body.innerText : ''",
                    )?.toString()
            }.getOrNull()
            if (cont.isActive) cont.resume(result)
        }
    }

private suspend fun snapshotOnFxThread(
    webView: WebView,
    widthPx: Int,
    heightPx: Int,
): ImageBitmap =
    suspendCancellableCoroutine { cont ->
        Platform.runLater {
            try {
                val target = WritableImage(widthPx, heightPx)
                val params = SnapshotParameters()
                // Async snapshot: JavaFX schedules it on the next render pulse, guaranteeing the
                // WebView has actually painted at least once (a synchronous snapshot on an off-screen
                // WebView often returns a blank image because no pulse has fired yet).
                webView.snapshot({ result ->
                    try {
                        val raw = SwingFXUtils.fromFXImage(result.image, null)
                        // SwingFXUtils returns TYPE_INT_ARGB_PRE; Compose's toComposeImageBitmap()
                        // does not unmultiply, so a direct conversion decodes as fully transparent
                        // on Skia. Repaint into a plain TYPE_INT_ARGB buffer first.
                        val normalized = java.awt.image.BufferedImage(
                            raw.width,
                            raw.height,
                            java.awt.image.BufferedImage.TYPE_INT_ARGB,
                        )
                        val g = normalized.createGraphics()
                        g.drawImage(raw, 0, 0, null)
                        g.dispose()
                        if (cont.isActive) cont.resume(normalized.toComposeImageBitmap())
                    } catch (t: Throwable) {
                        if (cont.isActive) cont.cancel(t)
                    }
                    null
                }, params, target)
            } catch (t: Throwable) {
                if (cont.isActive) cont.cancel(t)
            }
        }
    }
