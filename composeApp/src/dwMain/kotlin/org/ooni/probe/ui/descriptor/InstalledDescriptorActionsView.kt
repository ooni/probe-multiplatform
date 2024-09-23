package org.ooni.probe.ui.descriptor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.ooni.probe.data.models.InstalledTestDescriptorModel

@Composable
fun InstalledDescriptorActionsView(
    descriptor: InstalledTestDescriptorModel,
    onEvent: (DescriptorViewModel.Event) -> Unit,
    modifier: Modifier,
) {
}
