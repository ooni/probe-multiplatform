package org.ooni.probe.ui.measurement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.back
import ooniprobe.composeapp.generated.resources.measurement
import ooniprobe.composeapp.generated.resources.refresh
import org.intellij.markdown.html.urlEncode
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.ui.shared.OoniWebView
import org.ooni.probe.ui.shared.OoniWebViewController
import org.ooni.probe.ui.shared.TopBar

@Composable
fun MeasurementScreen(
    reportId: MeasurementModel.ReportId,
    input: String?,
    onBack: () -> Unit,
    onEvent: (MeasurementViewModel.Event) -> Unit,
) {
    val controller = remember { OoniWebViewController() }

    val inputSuffix = input?.let { "?input=${urlEncode(it)}" } ?: ""
    val url = "${OrganizationConfig.explorerUrl}/measurement/${reportId.value}$inputSuffix"

    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        Box {
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
                        onClick = {
                            onEvent(MeasurementViewModel.Event.ShareUrl(url))
                        },
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                        )
                    }
                    if (controller.state is OoniWebViewController.State.Loading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            trackColor = Color.Transparent,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .padding(12.dp)
                                .size(24.dp),
                        )
                    } else {
                        IconButton(
                            onClick = { controller.reload() },
                            enabled = controller.state is OoniWebViewController.State.Finished,
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(Res.string.refresh),
                            )
                        }
                    }
                },
            )

            LinearProgressIndicator(
                progress = {
                    (controller.state as? OoniWebViewController.State.Loading)?.progress ?: 0f
                },
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = Color.Transparent,
                drawStopIndicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .height(2.dp),
            )
        }

        OoniWebView(
            controller = controller,
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.navigationBars.asPaddingValues()),
        )
    }

    LaunchedEffect(url) {
        controller.load(url)
    }
}
