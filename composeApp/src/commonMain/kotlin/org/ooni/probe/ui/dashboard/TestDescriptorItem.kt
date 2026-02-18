package org.ooni.probe.ui.dashboard

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource
import org.ooni.probe.data.models.DescriptorItem
import org.ooni.probe.data.models.UpdateStatus
import org.ooni.probe.ui.shared.ExpiredChip
import org.ooni.probe.ui.shared.UpdatesChip

@Composable
fun TestDescriptorItem(
    descriptor: DescriptorItem,
    onClick: () -> Unit,
    onUpdateClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(CardDefaults.shape)
            .clickable { onClick() }
            .testTag(descriptor.key)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CardDefaults.shape,
            ).padding(vertical = 8.dp, horizontal = 12.dp),
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
        if (descriptor.updateStatus is UpdateStatus.Updatable) {
            UpdatesChip(onClick = onUpdateClick)
        }
        if (descriptor.isExpired) {
            ExpiredChip()
        }
        Icon(
            painter = painterResource(Res.drawable.ic_chevron_right),
            contentDescription = null,
        )
    }
}
