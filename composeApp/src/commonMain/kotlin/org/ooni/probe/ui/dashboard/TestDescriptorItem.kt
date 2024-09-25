package org.ooni.probe.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.ui.shared.UpdatesChip

@Composable
fun TestDescriptorItem(
    descriptor: Descriptor,
    onClick: () -> Unit,
    updateDescriptor: () -> Unit = {},
) {
    Card(
        Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
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
                TestDescriptorLabel(descriptor)

                descriptor.shortDescription()?.let { shortDescription ->
                    Text(
                        shortDescription,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            if (descriptor.updatable) {
                UpdatesChip(onClick = updateDescriptor)
            }
            Icon(
                painter = painterResource(Res.drawable.ic_chevron_right),
                contentDescription = null,
            )
        }
    }
}
