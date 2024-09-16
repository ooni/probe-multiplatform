package org.ooni.probe.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDate.Companion.Format
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Modal_Delete
import ooniprobe.composeapp.generated.resources.Modal_DoYouWantToDeleteAllTests
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Snackbar_ResultsNotUploaded_Text
import ooniprobe.composeapp.generated.resources.Snackbar_ResultsSomeNotUploaded_Text
import ooniprobe.composeapp.generated.resources.Snackbar_ResultsSomeNotUploaded_UploadAll
import ooniprobe.composeapp.generated.resources.TestResults_Overview_NoTestsHaveBeenRun
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Title
import ooniprobe.composeapp.generated.resources.TestResults_UnknownASN
import ooniprobe.composeapp.generated.resources.ic_cloud_off
import ooniprobe.composeapp.generated.resources.ic_delete_all
import ooniprobe.composeapp.generated.resources.measurements_count
import ooniprobe.composeapp.generated.resources.months
import ooniprobe.composeapp.generated.resources.ooni_empty_state
import ooniprobe.composeapp.generated.resources.task_origin_auto_run
import ooniprobe.composeapp.generated.resources.task_origin_manual
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.ResultListItem
import org.ooni.probe.ui.dashboard.TestDescriptorLabel
import org.ooni.probe.ui.shared.relativeDateTime

@Composable
fun ResultsScreen(
    state: ResultsViewModel.State,
    onEvent: (ResultsViewModel.Event) -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column {
        TopAppBar(
            title = {
                Text(stringResource(Res.string.TestResults_Overview_Title))
            },
            actions = {
                if (!state.isLoading && state.results.any()) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            painterResource(Res.drawable.ic_delete_all),
                            contentDescription = stringResource(Res.string.Modal_Delete),
                        )
                    }
                }
            },
        )

        if (state.anyMissingUpload) {
            UploadResults(onUploadClick = { onEvent(ResultsViewModel.Event.UploadClick) })
        }

        if (state.isLoading) {
            LoadingResults()
        } else if (state.results.isEmpty()) {
            EmptyResults()
        } else {
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

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            onConfirm = {
                onEvent(ResultsViewModel.Event.DeleteAllClick)
                showDeleteConfirm = false
            },
            onDismiss = {
                showDeleteConfirm = false
            },
        )
    }
}

@Composable
private fun UploadResults(onUploadClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(stringResource(Res.string.Snackbar_ResultsSomeNotUploaded_Text))
            TextButton(onClick = onUploadClick) {
                Text(stringResource(Res.string.Snackbar_ResultsSomeNotUploaded_UploadAll))
            }
        }
    }
}

@Composable
private fun LoadingResults() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyResults() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp)
            .padding(bottom = 120.dp) // Optical alignment
            .alpha(0.5f),
    ) {
        Icon(
            painterResource(Res.drawable.ooni_empty_state),
            contentDescription = null,
        )
        Text(
            stringResource(Res.string.TestResults_Overview_NoTestsHaveBeenRun),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
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
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                )

                Text(item.result.startTime.relativeDateTime())
            }
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(0.34f),
            ) {
                Text(
                    stringResource(
                        when (item.result.taskOrigin) {
                            TaskOrigin.AutoRun -> Res.string.task_origin_auto_run
                            TaskOrigin.OoniRun -> Res.string.task_origin_manual
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
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

@Composable
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        text = {
            Text(stringResource(Res.string.Modal_DoYouWantToDeleteAllTests))
        },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text(
                    stringResource(Res.string.Modal_Delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(Res.string.Modal_Cancel))
            }
        },
    )
}
