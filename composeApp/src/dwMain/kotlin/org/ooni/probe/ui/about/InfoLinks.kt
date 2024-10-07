package org.ooni.probe.ui.settings.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.logo_probe
import ooniprobe.composeapp.generated.resources.onboarding
import org.jetbrains.compose.resources.painterResource

@Composable
fun InfoLinks(launchUrl: (String) -> Unit) {
}

@Composable
fun InfoBackground(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.logo_probe),
        contentDescription = null,
        modifier = modifier.height(80.dp),
    )
}
