package org.ooni.probe.ui.results

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.test_results
import org.jetbrains.compose.resources.stringResource

@Composable
fun ResultsScreen(
    state: ResultsViewModel.State,
    onEvent: (ResultsViewModel.Event) -> Unit,
) {
    Column {
        TopAppBar(
            title = {
                Text(stringResource(Res.string.test_results))
            },
        )

        state.results.forEach { result ->
            Button(onClick = { onEvent(ResultsViewModel.Event.ResultClick(result)) }) {
                Text(result.id.value)
            }
        }
    }
}
