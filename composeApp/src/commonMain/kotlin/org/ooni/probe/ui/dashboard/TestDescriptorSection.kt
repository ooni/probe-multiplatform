package org.ooni.probe.ui.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ooniprobe.composeapp.generated.resources.Dashboard_RunV2_Ooni_Title
import ooniprobe.composeapp.generated.resources.Dashboard_RunV2_Title
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.DescriptorType

@Composable
fun TestDescriptorSection(
    type: DescriptorType,
    modifier: Modifier = Modifier,
) {
    Text(
        stringResource(
            when (type) {
                DescriptorType.Default -> Res.string.Dashboard_RunV2_Ooni_Title
                DescriptorType.Installed -> Res.string.Dashboard_RunV2_Title
            },
        ).uppercase(),
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier,
    )
}
