package org.ooni.probe.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Dashboard_Progress_UpdateLink_Label
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.UpdateStatusType


@Composable
fun UpdateStatus(modifier: Modifier, type: UpdateStatusType) {
    Row(
        modifier = modifier.fillMaxWidth()
            .height(56.dp)
            .background(Color.Red)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator()
        Text(stringResource(Res.string.Dashboard_Progress_UpdateLink_Label))
    }
}
