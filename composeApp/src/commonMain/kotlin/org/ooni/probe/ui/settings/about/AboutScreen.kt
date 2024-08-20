package org.ooni.probe.ui.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_About_Content_Paragraph
import ooniprobe.composeapp.generated.resources.Settings_About_Label
import ooniprobe.composeapp.generated.resources.back
import org.jetbrains.compose.resources.stringResource

@Composable
fun AboutScreen(onEvent: (AboutViewModel.Event) -> Unit) {
    LazyColumn {
        item {
            LargeTopAppBar(
                title = {
                    Text(stringResource(Res.string.Settings_About_Label))
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(AboutViewModel.Event.BackClicked) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(Res.string.Settings_About_Content_Paragraph))
                Spacer(modifier = Modifier.height(16.dp))
                InfoLinks(
                    launchUrl = { url -> onEvent(AboutViewModel.Event.LaunchUrlClicked(url)) },
                )
            }
        }
    }
}
