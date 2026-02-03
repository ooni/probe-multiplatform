package org.ooni.probe.ui.shared

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.WebViewProgressIndicator(progress: Float) {
    val progressColor = MaterialTheme.colorScheme.onPrimary
    val progressTrackColor = Color.Transparent
    val progressModifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .padding(bottom = 2.dp)
        .height(2.dp)

    if (progress == 0f) {
        LinearProgressIndicator(
            color = progressColor,
            trackColor = progressTrackColor,
            modifier = progressModifier,
        )
    } else {
        LinearProgressIndicator(
            progress = { progress },
            color = progressColor,
            trackColor = progressTrackColor,
            drawStopIndicator = {},
            modifier = progressModifier,
        )
    }
}
