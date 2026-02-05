package org.ooni.probe.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.ooni_empty_state
import org.jetbrains.compose.resources.painterResource
import org.ooni.probe.data.models.DescriptorItem

@Composable
fun TestDescriptorLabel(
    descriptor: DescriptorItem,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(descriptor.icon ?: Res.drawable.ooni_empty_state),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(24.dp)
                .background(
                    color = descriptor.color ?: MaterialTheme.colorScheme.onSurface.copy(0.75f),
                    shape = CircleShape,
                ).padding(4.dp),
        )
        Text(
            descriptor.title(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
