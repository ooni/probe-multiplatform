package org.ooni.probe.ui.settings.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_About_Content_Blog
import ooniprobe.composeapp.generated.resources.Settings_About_Content_DataPolicy
import ooniprobe.composeapp.generated.resources.Settings_About_Content_LearnMore
import ooniprobe.composeapp.generated.resources.Settings_About_Content_Reports
import ooniprobe.composeapp.generated.resources.ooni_logo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.ui.shared.isHeightCompact

@Composable
fun InfoLinks(launchUrl: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(onClick = {
            launchUrl("https://ooni.org/")
        }) {
            Text(stringResource(Res.string.Settings_About_Content_LearnMore))
        }
        TextButton(onClick = {
            launchUrl("https://ooni.org/blog/")
        }) {
            Text(stringResource(Res.string.Settings_About_Content_Blog))
        }
        TextButton(onClick = {
            launchUrl("https://ooni.org/reports/")
        }) {
            Text(stringResource(Res.string.Settings_About_Content_Reports))
        }
        TextButton(onClick = {
            launchUrl("https://ooni.org/about/data-policy/")
        }) {
            Text(stringResource(Res.string.Settings_About_Content_DataPolicy))
        }
    }
}

@Composable
fun InfoBackground(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.ooni_logo),
        contentDescription = null,
        modifier = modifier.fillMaxWidth()
            .heightIn(max = if (isHeightCompact()) 48.dp else Dp.Unspecified),
    )
}
