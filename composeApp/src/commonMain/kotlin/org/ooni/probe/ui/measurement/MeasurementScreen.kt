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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewState
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.back
import ooniprobe.composeapp.generated.resources.measurement
import ooniprobe.composeapp.generated.resources.refresh
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.ui.shared.ColorDefaults

@Composable
fun MeasurementScreen(
    reportId: MeasurementModel.ReportId,
    input: String?,
    onBack: () -> Unit,
) {
    val inputSuffix = input?.let { "?input=$it" } ?: ""
    val url = "https://explorer.ooni.org/measurement/${reportId.value}$inputSuffix"

    val webViewState = rememberWebViewState(url)
    val webViewNavigator = rememberWebViewNavigator()

    Column {
        TopAppBar(
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
            colors = ColorDefaults.topAppBar(),
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
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.navigationBars.asPaddingValues()),
        )
    }
}
