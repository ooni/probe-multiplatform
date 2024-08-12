package org.ooni.probe.ui.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Dashboard_RunV2_Ooni_Title
import ooniprobe.composeapp.generated.resources.Dashboard_RunV2_Title
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource

@Composable
fun TestDescriptorItem(type: DashboardViewModel.DescriptorType) {
    Text(
        stringResource(
            when (type) {
                DashboardViewModel.DescriptorType.Default -> Res.string.Dashboard_RunV2_Ooni_Title
                DashboardViewModel.DescriptorType.Installed -> Res.string.Dashboard_RunV2_Title
            },
        ).uppercase(),
        style = MaterialTheme.typography.labelLarge,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 4.dp),
    )
}
