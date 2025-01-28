package org.ooni.probe.ui.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Dashboard_Progress_ReviewLink_Action
import ooniprobe.composeapp.generated.resources.Dashboard_Progress_ReviewLink_Label
import ooniprobe.composeapp.generated.resources.Dashboard_Progress_UpdateLink_Label
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.DescriptorUpdateOperationState

@Composable
fun UpdateProgressStatus(
    type: DescriptorUpdateOperationState,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    onReviewLinkClicked: () -> Unit = {},
    onCancelClicked: () -> Unit = {},
) {
    Surface(
        color = MaterialTheme.colorScheme.inverseSurface,
        modifier = modifier,
    ) {
        Row(
            modifier = contentModifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = if (type == DescriptorUpdateOperationState.FetchingUpdates) {
                Arrangement.spacedBy(10.dp)
            } else {
                Arrangement.SpaceBetween
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (type == DescriptorUpdateOperationState.FetchingUpdates) {
                CircularProgressIndicator()
                Text(stringResource(Res.string.Dashboard_Progress_UpdateLink_Label))
            } else if (type == DescriptorUpdateOperationState.ReviewNecessaryNotice) {
                Text(stringResource(Res.string.Dashboard_Progress_ReviewLink_Label))
                Row {
                    TextButton(onClick = onReviewLinkClicked) {
                        Text(stringResource(Res.string.Dashboard_Progress_ReviewLink_Action))
                    }
                    IconButton(onClick = onCancelClicked) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(Res.string.Modal_Cancel),
                        )
                    }
                }
            }
        }
    }
}
