package org.ooni.probe.ui.settings.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.onboarding
import org.jetbrains.compose.resources.painterResource

@Composable
fun InfoLinks(launchUrl: (String) -> Unit) {
}

@Composable
fun InfoBackground(modifier: Modifier) {
    Box(modifier = modifier.background(Color(0xFFD32625))) {
        Image(
            painter = painterResource(Res.drawable.onboarding),
            contentDescription = null,
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
        )
    }
}
