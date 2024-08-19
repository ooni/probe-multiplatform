package org.ooni.probe.ui.dashboard

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.ic_settings
import org.jetbrains.compose.resources.painterResource
import org.ooni.probe.data.models.Descriptor

@Composable
fun TestDescriptorLabel(descriptor: Descriptor) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 2.dp),
    ) {
        Icon(
            // TODO: pick better fallback icon
            painter = painterResource(descriptor.icon ?: Res.drawable.ic_settings),
            contentDescription = null,
            tint = descriptor.color ?: Color.Unspecified,
            modifier =
                Modifier
                    .size(24.dp)
                    .padding(end = 4.dp),
        )
        Text(
            descriptor.title(),
            style = MaterialTheme.typography.titleMedium,
            color = descriptor.color ?: Color.Unspecified,
        )
    }
}
