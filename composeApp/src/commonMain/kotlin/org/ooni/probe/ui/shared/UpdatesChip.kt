package org.ooni.probe.ui.shared

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ooniprobe.composeapp.generated.resources.Dashboard_RunV2_UpdateTag
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource

@Composable
fun UpdatesChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SuggestionChip(
        onClick = onClick,
        colors = SuggestionChipDefaults.suggestionChipColors(
            labelColor = LocalContentColor.current,
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = LocalContentColor.current,
        ),
        label = { Text(stringResource(Res.string.Dashboard_RunV2_UpdateTag)) },
        modifier = modifier,
    )
}
