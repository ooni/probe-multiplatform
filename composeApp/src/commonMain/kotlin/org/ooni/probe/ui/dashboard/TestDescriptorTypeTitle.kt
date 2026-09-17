package org.ooni.probe.ui.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.DescriptorType
import org.ooni.probe.data.models.titleRes

@Composable
fun TestDescriptorTypeTitle(
    type: DescriptorType,
    modifier: Modifier = Modifier,
) {
    Text(
        stringResource(type.titleRes).uppercase(),
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier,
    )
}
