package org.ooni.probe.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDate.Companion.Format
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Snackbar_ResultsNotUploaded_Text
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Title
import ooniprobe.composeapp.generated.resources.TestResults_UnknownASN
import ooniprobe.composeapp.generated.resources.ic_cloud_off
import ooniprobe.composeapp.generated.resources.measurements_count
import ooniprobe.composeapp.generated.resources.months
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.ResultListItem
import org.ooni.probe.ui.dashboard.TestDescriptorLabel

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
                stickyHeader(key = date.toString()) {
                    ResultDateHeader(date)
                }
                items(items = results) { result ->
                    ResultItem(
                        item = result,
                        onResultClick = { onEvent(ResultsViewModel.Event.ResultClick(result)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultDateHeader(date: LocalDate) {
    val monthNames = stringArrayResource(Res.array.months)
    Text(
        date.format(
            Format {
                monthName(MonthNames(monthNames))
                char(' ')
                year()
            },
        ),
        style = MaterialTheme.typography.labelLarge,
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun ResultItem(
    item: ResultListItem,
    onResultClick: () -> Unit,
) {
    Surface(
        color = if (item.result.isViewed) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier.padding(top = 1.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onResultClick() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Column(
                modifier = Modifier.weight(0.66f),
            ) {
                TestDescriptorLabel(item.descriptor)

                Text(
                    item.network?.networkName ?: stringResource(Res.string.TestResults_UnknownASN),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 3,
                )

                Text(
                    item.result.startTime.format(
                        LocalDateTime.Format {
                            date(LocalDate.Formats.ISO)
                            char(' ')
                            hour()
                            char(':')
                            minute()
                            char(':')
                            second()
                        },
                    ),
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(0.34f),
            ) {
                Text(
                    pluralStringResource(
                        Res.plurals.measurements_count,
                        item.measurementsCount.toInt(),
                        item.measurementsCount,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
                if (!item.allMeasurementsUploaded) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_cloud_off),
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp),
                        )
                        Text(
                            stringResource(Res.string.Snackbar_ResultsNotUploaded_Text).lowercase(),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}
