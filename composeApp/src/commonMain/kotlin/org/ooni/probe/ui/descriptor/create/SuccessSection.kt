package org.ooni.probe.ui.descriptor.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.CreateDescriptor_AppLink
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Copy
import ooniprobe.composeapp.generated.resources.CreateDescriptor_ShareLink
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Success
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.ui.descriptor.create.CreateDescriptorViewModel.Event

@Composable
internal fun SuccessSection(
    state: CreateDescriptorViewModel.State,
    onEvent: (Event) -> Unit,
) {
    Text(
        stringResource(Res.string.CreateDescriptor_Success),
        modifier = Modifier.padding(bottom = 16.dp),
    )

    state.runLink?.let { runLink ->
        CopyableLink(
            label = stringResource(Res.string.CreateDescriptor_ShareLink),
            value = runLink,
            testTag = "CreateDescriptor-RunLink",
            onEvent = onEvent,
        )
    }
    state.deepLink?.let { deepLink ->
        CopyableLink(
            label = stringResource(Res.string.CreateDescriptor_AppLink),
            value = deepLink,
            testTag = "CreateDescriptor-DeepLink",
            onEvent = onEvent,
        )
    }
}

@Composable
private fun CopyableLink(
    label: String,
    value: String,
    testTag: String,
    onEvent: (Event) -> Unit,
) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = value,
                modifier = Modifier
                    .weight(1f)
                    .testTag(testTag),
            )
            TextButton(onClick = { onEvent(Event.CopyClicked(value)) }) {
                Text(stringResource(Res.string.CreateDescriptor_Copy))
            }
        }
    }
}
