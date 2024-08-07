package org.ooni.probe.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDate.Companion.Format
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Title
import ooniprobe.composeapp.generated.resources.TestResults_UnknownASN
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.ResultListItem

@Composable
fun ResultsScreen(
    state: ResultsViewModel.State,
    onEvent: (ResultsViewModel.Event) -> Unit,
) {
    Column {
        TopAppBar(
            title = {
                Text(stringResource(Res.string.TestResults_Overview_Title))
            },
        )

        LazyColumn {
            state.results.forEach { (date, results) ->
                stickyHeader(key = date) {
                    ResultDateHeader(date)
                }
                items(items = results, key = { it.idOrThrow }) { result ->
                    ResultItem(
                        item = result,
                        onResultClick = { onEvent(ResultsViewModel.Event.ResultClick(result)) }
                    )
                }
            }
        }
    }
}

@Composable
fun ResultDateHeader(date: LocalDate) {
    Text(
        date.format(Format { year(); char('-'); monthNumber() }),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
fun ResultItem(
    item: ResultListItem,
    onResultClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onResultClick() }
            .background(
                if (item.result.isViewed) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
    ) {
        Text(item.result.testGroupName.orEmpty())
        Text(item.network?.networkName ?: stringResource(Res.string.TestResults_UnknownASN))
        Text(item.result.startTime.toString())
    }
}
