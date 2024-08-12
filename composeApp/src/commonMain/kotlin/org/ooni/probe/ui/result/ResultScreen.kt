package org.ooni.probe.ui.result

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.back
import org.jetbrains.compose.resources.stringResource

@Composable
fun ResultScreen(
    state: ResultViewModel.State,
    onEvent: (ResultViewModel.Event) -> Unit,
) {
    Column {
        TopAppBar(
            title = {
                Text(state.result?.testGroupName.orEmpty())
            },
            navigationIcon = {
                IconButton(onClick = { onEvent(ResultViewModel.Event.BackClicked) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                    )
                }
            },
        )
    }
}
