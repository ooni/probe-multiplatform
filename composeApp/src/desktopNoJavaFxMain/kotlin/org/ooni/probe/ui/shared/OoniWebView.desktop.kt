package org.ooni.probe.ui.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger
internal var ooniWebViewOverride: (
    @Composable (
        controller: OoniWebViewController,
        modifier: Modifier,
        allowedDomains: List<String>,
        onDisallowedUrl: (String) -> Unit,
    ) -> Unit
)? = null

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
