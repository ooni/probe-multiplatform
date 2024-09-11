package org.ooni.probe.ui.descriptor

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.ooni.probe.data.models.InstalledTestDescriptorModel

@Composable
fun InstalledDescriptorActionsView(
    value: InstalledTestDescriptorModel,
    onEvent: (DescriptorViewModel.Event) -> Unit
) {
}
