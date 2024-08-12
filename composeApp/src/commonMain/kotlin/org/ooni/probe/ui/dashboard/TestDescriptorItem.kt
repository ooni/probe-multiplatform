package org.ooni.probe.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.ic_chevron_right
import ooniprobe.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource
import org.ooni.probe.data.models.Descriptor

@Composable
fun TestDescriptorItem(descriptor: Descriptor) {
    Card(
        Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 2.dp),
                ) {
                    Icon(
                        painter = painterResource(descriptor.icon ?: Res.drawable.logo),
                        contentDescription = null,
                        tint = descriptor.color ?: MaterialTheme.colorScheme.onSurface,
                        modifier =
                            Modifier
                                .size(24.dp)
                                .padding(end = 4.dp),
                    )
                    Text(
                        descriptor.title(),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                descriptor.shortDescription()?.let { shortDescription ->
                    Text(
                        shortDescription,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Icon(
                painter = painterResource(Res.drawable.ic_chevron_right),
                contentDescription = null,
            )
        }
    }
}
