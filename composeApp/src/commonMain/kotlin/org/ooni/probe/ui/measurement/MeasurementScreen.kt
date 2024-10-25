package org.ooni.probe.ui.measurement

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import com.multiplatform.webview.request.RequestInterceptor
import com.multiplatform.webview.request.WebRequest
import com.multiplatform.webview.request.WebRequestInterceptResult
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.back
import ooniprobe.composeapp.generated.resources.measurement
import ooniprobe.composeapp.generated.resources.refresh
import org.intellij.markdown.html.urlEncode
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.ui.shared.TopBar

@Composable
fun MeasurementScreen(
    reportId: MeasurementModel.ReportId,
    input: String?,
    onBack: () -> Unit,
) {
    val inputSuffix = input?.let { "?input=${urlEncode(it)}" } ?: ""
    val url = "https://explorer.ooni.org/measurement/${reportId.value}$inputSuffix"
    LaunchedEffect(url) { Logger.i("URL: $url") }

    val webViewState = rememberWebViewState(url)
    webViewState.webSettings.isJavaScriptEnabled = false
    val webViewNavigator = rememberWebViewNavigator(
        // Don't allow other links to open
        requestInterceptor = object : RequestInterceptor {
            override fun onInterceptUrlRequest(
                request: WebRequest,
                navigator: WebViewNavigator,
            ) = if (request.url.startsWith("https://explorer.ooni.org/measurement/") ||
                request.url.startsWith("https://explorer.ooni.org/m/")
            ) {
                WebRequestInterceptResult.Allow
            } else {
                WebRequestInterceptResult.Reject
            }
        },
    )

    Column {
        TopBar(
            title = {
                Text(stringResource(Res.string.measurement))
            },
            navigationIcon = {
                IconButton(onClick = { onBack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { webViewNavigator.reload() },
                    enabled = webViewState.loadingState is LoadingState.Finished,
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(Res.string.refresh),
                    )
                }
            },
        )

        LinearProgressIndicator(
            progress = {
                val loadingState = webViewState.loadingState
                if (loadingState is LoadingState.Loading) loadingState.progress else 1f
            },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp),
        )

        WebView(
            state = webViewState,
            navigator = webViewNavigator,
            captureBackPresses = false,
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.navigationBars.asPaddingValues()),
        )
    }
}
