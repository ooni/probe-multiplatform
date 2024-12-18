package org.ooni.probe.ui.shared

import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ooniprobe.composeapp.generated.resources.Dashboard_RunV2_ExpiredTag
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExpiredChip(modifier: Modifier = Modifier) {
    SuggestionChip(
        onClick = { },
        enabled = false,
        label = { Text(stringResource(Res.string.Dashboard_RunV2_ExpiredTag)) },
        modifier = modifier,
    )
}
