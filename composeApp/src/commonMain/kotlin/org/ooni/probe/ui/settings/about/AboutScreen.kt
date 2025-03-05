package org.ooni.probe.ui.settings.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Common_Back
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_About_Content_Paragraph
import ooniprobe.composeapp.generated.resources.version
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.ui.shared.MarkdownViewer
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.shared.isHeightCompact

@Composable
fun AboutScreen(
    onEvent: (AboutViewModel.Event) -> Unit,
    softwareName: String,
    softwareVersion: String,
) {
    Column(
        Modifier
            .background(MaterialTheme.colorScheme.background)
            .testTag("AboutScreen")
    ) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TopBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { onEvent(AboutViewModel.Event.BackClicked) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.Common_Back),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        navigationIconContentColor = LocalContentColor.current,
                    ),
                )

                InfoBackground(
                    modifier = Modifier.padding(bottom = if (isHeightCompact()) 8.dp else 32.dp),
                )

                Text(
                    text = stringResource(Res.string.version, softwareName, softwareVersion),
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .padding(bottom = 32.dp),
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
