package org.ooni.probe.ui.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import ooniprobe.composeapp.generated.resources.Dashboard_Articles_OpenExternal
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Donate_Action_Warning_ExternalBrowser
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.PlatformAction
import org.ooni.probe.openUrl
import java.net.URI

internal var ooniWebViewOverride: (
    @Composable (
        controller: OoniWebViewController,
        modifier: Modifier,
        allowedDomains: List<String>,
        onDisallowedUrl: (String) -> Unit,
    ) -> Unit
)? = null

/**
 * Mac App Store desktop build of [OoniWebView].
 *
 * The App Store channel ships without JavaFX (its WebView native libraries
 * can't satisfy the hardened-runtime library validation the store enforces),
 * so there is no embedded browser to render. Instead, an [OoniWebViewController.Event.Load]
 * opens the (allow-listed) URL in the user's system browser via the same
 * [openUrl] path used elsewhere on desktop, and the screen shows a short
 * notice with a button to re-open the link.
 *
 * This file references no `javafx.*` symbols, so the App Store classpath
 * never loads a JavaFX class — avoiding `NoClassDefFoundError` even though the
 * JavaFX jars are absent.
 */
@Composable
actual fun OoniWebView(
    controller: OoniWebViewController,
    modifier: Modifier,
    allowedDomains: List<String>,
    onDisallowedUrl: (String) -> Unit,
) {
    ooniWebViewOverride?.let { override ->
        override(controller, modifier, allowedDomains, onDisallowedUrl)
        return
    }

    val event = controller.rememberNextEvent()
    var lastUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(event) {
        when (val current = event) {
            is OoniWebViewController.Event.Load -> {
                lastUrl = current.url
                openInExternalBrowser(current.url, allowedDomains, onDisallowedUrl, controller)
                controller.onEventHandled(current)
            }
            // Reload re-opens the last URL; Back is a no-op without history.
            OoniWebViewController.Event.Reload -> {
                lastUrl?.let { openInExternalBrowser(it, allowedDomains, onDisallowedUrl, controller) }
                controller.onEventHandled(current)
            }
            OoniWebViewController.Event.Back -> controller.onEventHandled(current)
            null -> Unit
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.Settings_Donate_Action_Warning_ExternalBrowser),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(
            onClick = {
                lastUrl?.let { openInExternalBrowser(it, allowedDomains, onDisallowedUrl, controller) }
            },
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(
                stringResource(Res.string.Dashboard_Articles_OpenExternal),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

/**
 * Applies the same allow-list check as the embedded WebView before handing the
 * URL to the system browser. Disallowed URLs are reported through
 * [onDisallowedUrl] and never opened.
 */
private fun openInExternalBrowser(
    url: String,
    allowedDomains: List<String>,
    onDisallowedUrl: (String) -> Unit,
    controller: OoniWebViewController,
) {
    val allowed = try {
        val host = URI.create(url).host
        host != null && allowedDomains.any { domain -> host.matches(Regex("^(.*\\.)?$domain$")) }
    } catch (e: Exception) {
        Logger.w("Invalid URL $url, not opening", e)
        false
    }

    if (!allowed) {
        onDisallowedUrl(url)
        controller.state = OoniWebViewController.State.Failure
        return
    }

    val opened = runCatching { openUrl(PlatformAction.OpenUrl(url)) }
        .getOrElse { e ->
            Logger.w("Failed to open $url in external browser", e)
            false
        }
    controller.state = if (opened) {
        OoniWebViewController.State.Successful
    } else {
        OoniWebViewController.State.Failure
    }
}
