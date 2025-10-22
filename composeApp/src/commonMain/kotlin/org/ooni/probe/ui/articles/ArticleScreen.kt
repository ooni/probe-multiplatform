package org.ooni.probe.ui.articles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Common_Refresh
import ooniprobe.composeapp.generated.resources.Dashboard_Articles_Title
import ooniprobe.composeapp.generated.resources.Measurement_LoadingFailed
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.ic_cloud_off
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.ui.shared.NavigationBackButton
import org.ooni.probe.ui.shared.OoniWebView
import org.ooni.probe.ui.shared.OoniWebViewController
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.shared.WebViewProgressIndicator

@Composable
fun ArticleScreen(
    state: ArticleViewModel.State,
    onEvent: (ArticleViewModel.Event) -> Unit,
) {
    val controller = remember { OoniWebViewController() }

    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        Box {
            TopBar(
                title = {
                    Text(stringResource(Res.string.Dashboard_Articles_Title))
                },
                navigationIcon = {
                    NavigationBackButton({ onEvent(ArticleViewModel.Event.BackClicked) })
                },
                actions = {
                    IconButton(onClick = { onEvent(ArticleViewModel.Event.ShareUrl) }) {
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
                            enabled = controller.state.isFinished,
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(Res.string.Common_Refresh),
                            )
                        }
                    }
                },
            )

            if (controller.state is OoniWebViewController.State.Initializing ||
                controller.state is OoniWebViewController.State.Loading
            ) {
                WebViewProgressIndicator(
                    (controller.state as? OoniWebViewController.State.Loading)?.progress ?: 0f,
                )
            }
        }

        if (state !is ArticleViewModel.State.Show) return@Column

        Box(modifier = Modifier.fillMaxSize()) {
            val isFailure = controller.state is OoniWebViewController.State.Failure

            OoniWebView(
                controller = controller,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (isFailure) 0f else 1f)
                    .padding(WindowInsets.navigationBars.asPaddingValues()),
                onDisallowedUrl = { onEvent(ArticleViewModel.Event.OutsideLinkClicked(it)) },
            )

            if (isFailure) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_cloud_off),
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 32.dp).size(48.dp),
                    )
                    Text(
                        text = stringResource(Res.string.Measurement_LoadingFailed),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    OutlinedButton(
                        onClick = { controller.reload() },
                        modifier = Modifier.padding(top = 32.dp),
                    ) {
                        Text(
                            stringResource(Res.string.Common_Refresh),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }
    }

    val url = (state as? ArticleViewModel.State.Show)?.url
    LaunchedEffect(url) {
        url?.let(controller::load)
    }
}
