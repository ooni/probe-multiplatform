package org.ooni.probe.ui.settings.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.logo_probe
import org.jetbrains.compose.resources.painterResource
import org.ooni.probe.ui.shared.isHeightCompact

@Composable
fun InfoLinks(launchUrl: (String) -> Unit) {
}

@Composable
fun InfoBackground(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.logo_probe),
        contentDescription = null,
        modifier = modifier.height(if (isHeightCompact()) 48.dp else 80.dp),
    )
}
