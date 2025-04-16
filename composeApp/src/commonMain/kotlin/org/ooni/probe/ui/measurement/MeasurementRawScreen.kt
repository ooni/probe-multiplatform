package org.ooni.probe.ui.measurement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
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
import ooniprobe.composeapp.generated.resources.Common_Back
import ooniprobe.composeapp.generated.resources.Measurement_Raw_NotUploadedReasoning
import ooniprobe.composeapp.generated.resources.Measurement_Raw_Upload
import ooniprobe.composeapp.generated.resources.Measurement_Title
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.ui.shared.TopBar

@Composable
fun MeasurementRawScreen(
    state: MeasurementRawViewModel.State,
    onEvent: (MeasurementRawViewModel.Event) -> Unit,
) {
    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        TopBar(
            title = {
                Text(stringResource(Res.string.Measurement_Title))
            },
            navigationIcon = {
                IconButton(onClick = { onEvent(MeasurementRawViewModel.Event.BackClicked) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.Common_Back),
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        onEvent(MeasurementRawViewModel.Event.ShareClicked)
                    },
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                    )
                }
            },
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = 2.dp,
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    stringResource(Res.string.Measurement_Raw_NotUploadedReasoning),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                )
                TextButton(onClick = { onEvent(MeasurementRawViewModel.Event.UploadClicked) }) {
                    Text(stringResource(Res.string.Measurement_Raw_Upload))
                }
            }
        }

        Text(
            text = state.json.orEmpty(),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        )
    }
}
