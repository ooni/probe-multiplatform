package org.ooni.probe.ui.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_About_Content_Paragraph
import ooniprobe.composeapp.generated.resources.back
import ooniprobe.composeapp.generated.resources.version
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.ui.shared.MarkdownViewer

@Composable
fun AboutScreen(
    onEvent: (AboutViewModel.Event) -> Unit,
    softwareName: String,
    softwareVersion: String,
) {
    LazyColumn(modifier = Modifier.padding(WindowInsets.navigationBars.asPaddingValues())) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.TopStart),
            ) {
                InfoBackground(
                    modifier = Modifier.align(Alignment.BottomCenter).height(200.dp).fillMaxWidth(),
                )
                Text(
                    text = stringResource(Res.string.version, softwareName, softwareVersion),
                    color = Color.White,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                )

                LargeTopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { onEvent(AboutViewModel.Event.BackClicked) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.back),
                                tint = Color.White,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
                    ),
                )
            }
        }
        item {
            Column(
                verticalArrangement = Arrangement.Center,
            ) {
                MarkdownViewer(
                    markdown = stringResource(Res.string.Settings_About_Content_Paragraph),
                    modifier = Modifier.padding(16.dp),
                )

                Spacer(modifier = Modifier.height(16.dp))
                InfoLinks(
                    launchUrl = { url -> onEvent(AboutViewModel.Event.LaunchUrlClicked(url)) },
                )
            }
        }
    }
}
