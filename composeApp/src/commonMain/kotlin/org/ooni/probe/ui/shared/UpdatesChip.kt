package org.ooni.probe.ui.shared

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import ooniprobe.composeapp.generated.resources.Dashboard_RunV2_UpdatedTag
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource

@Composable
fun UpdatesChip(onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        colors = SuggestionChipDefaults.suggestionChipColors(
            labelColor = MaterialTheme.colorScheme.error,
        ),
        label = { Text(stringResource(Res.string.Dashboard_RunV2_UpdatedTag)) },
    )
}
