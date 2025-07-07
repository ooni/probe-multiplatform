package org.ooni.probe.ui.shared

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ooniprobe.composeapp.generated.resources.Dashboard_RunV2_ExpiredTag
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource

@Composable
fun ExpiredChip(modifier: Modifier = Modifier) {
    val contentColor = LocalContentColor.current.copy(alpha = 0.66f)
    SuggestionChip(
        onClick = {},
        enabled = false,
        label = { Text(stringResource(Res.string.Dashboard_RunV2_ExpiredTag)) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            disabledLabelColor = contentColor,
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = false,
            disabledBorderColor = contentColor,
        ),
        modifier = modifier,
    )
}
