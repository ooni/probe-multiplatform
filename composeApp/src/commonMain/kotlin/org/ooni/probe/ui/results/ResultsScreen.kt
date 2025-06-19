package org.ooni.probe.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDate.Companion.Format
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import ooniprobe.composeapp.generated.resources.Common_Yes
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Modal_Delete
import ooniprobe.composeapp.generated.resources.Modal_DoYouWantToDeleteAllTests
import ooniprobe.composeapp.generated.resources.Modal_DoYouWantToDeleteSomeTests
import ooniprobe.composeapp.generated.resources.Modal_Selected
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Results_LimitedNotice
import ooniprobe.composeapp.generated.resources.Results_MarkAllAsViewed
import ooniprobe.composeapp.generated.resources.Results_MarkAllAsViewed_Confirmation
import ooniprobe.composeapp.generated.resources.Results_MarkAllAsViewed_Filtered_Confirmation
import ooniprobe.composeapp.generated.resources.Settings_Websites_Categories_Selection_All
import ooniprobe.composeapp.generated.resources.Settings_Websites_Categories_Selection_None
import ooniprobe.composeapp.generated.resources.Snackbar_ResultsSomeNotUploaded_Text
import ooniprobe.composeapp.generated.resources.Snackbar_ResultsSomeNotUploaded_UploadAll
import ooniprobe.composeapp.generated.resources.TestResults_Filter_DeleteConfirmation
import ooniprobe.composeapp.generated.resources.TestResults_Filter_NoTestsFound
import ooniprobe.composeapp.generated.resources.TestResults_Filters_Title
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Hero_DataUsage
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Hero_Networks
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Hero_Tests
import ooniprobe.composeapp.generated.resources.TestResults_Overview_NoTestsHaveBeenRun
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Title
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Performance_Hero_Download
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Performance_Hero_Upload
import ooniprobe.composeapp.generated.resources.ic_delete_all
import ooniprobe.composeapp.generated.resources.ic_download
import ooniprobe.composeapp.generated.resources.ic_filters
import ooniprobe.composeapp.generated.resources.ic_mark_as_viewed
import ooniprobe.composeapp.generated.resources.ic_upload
import ooniprobe.composeapp.generated.resources.ooni_empty_state
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.ResultFilter
import org.ooni.probe.shared.stringMonthArrayResource
import org.ooni.probe.ui.shared.LightStatusBars
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.shared.formatDataUsage
import org.ooni.probe.ui.shared.isHeightCompact

@Composable
fun ResultsScreen(
    state: ResultsViewModel.State,
    onEvent: (ResultsViewModel.Event) -> Unit,
) {
    LightStatusBars(state.selectionEnabled && MaterialTheme.colorScheme.background.luminance() > 0.5f)

    var showFiltersDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMarkAsViewedConfirm by remember { mutableStateOf(false) }

    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        if (!state.selectionEnabled) {
            TopBar(
                title = {
                    Text(stringResource(Res.string.TestResults_Overview_Title))
                },
                actions = {
                    IconButton(
                        onClick = { showFiltersDialog = true },
                        enabled = state.results.any() || !state.filter.isAll,
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_filters),
                            contentDescription = stringResource(Res.string.TestResults_Filters_Title),
                        )
                    }
                    IconButton(
                        onClick = { showMarkAsViewedConfirm = true },
                        enabled = state.markAllAsViewedEnabled && !state.isLoading && state.results.any(),
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_mark_as_viewed),
                            contentDescription = stringResource(Res.string.Results_MarkAllAsViewed),
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        enabled = !state.isLoading && state.results.any(),
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_delete_all),
                            contentDescription = stringResource(Res.string.Modal_Delete),
                        )
                    }
                },
            )

            Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                ResultFiltersRow(
                    filter = state.filter,
                    onOpen = { showFiltersDialog = true },
                )
            }
        } else {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                navigationIcon = {
                    IconButton(onClick = { onEvent(ResultsViewModel.Event.CancelSelection) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.Modal_Cancel),
                        )
                    }
                },
                title = {
                    Text(
                        pluralStringResource(
                            Res.plurals.Modal_Selected,
                            state.selectedResultsCount,
                            state.selectedResultsCount,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                },
                actions = {
                    if (state.isAnySelected) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                painterResource(Res.drawable.ic_delete_all),
                                contentDescription = stringResource(Res.string.Modal_Delete),
                            )
                        }
                    }
                },
            )
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                val toggleState = if (state.areAllSelected) {
                    ToggleableState.On
                } else if (state.isAnySelected) {
                    ToggleableState.Indeterminate
                } else {
                    ToggleableState.Off
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .triStateToggleable(
                            state = toggleState,
                            onClick = {
                                onEvent(ResultsViewModel.Event.ToggleSelection)
                            },
                            role = Role.Checkbox,
                        )
                        .padding(8.dp),
                ) {
                    TriStateCheckbox(
                        state = toggleState,
                        onClick = null,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Text(
                        stringResource(
                            if (toggleState != ToggleableState.On) {
                                Res.string.Settings_Websites_Categories_Selection_All
                            } else {
                                Res.string.Settings_Websites_Categories_Selection_None
                            },
                        ),
                    )
                }
            }
        }
        if (showFiltersDialog) {
            ResultFiltersDialog(
                initialFilter = state.filter,
                descriptors = state.descriptors,
                onSave = {
                    onEvent(ResultsViewModel.Event.FilterChanged(it))
                    showFiltersDialog = false
                },
                onDismiss = { showFiltersDialog = false },
            )
        }

        if (state.isLoading) {
            LoadingResults()
        } else if (state.results.isEmpty()) {
            EmptyResults(anyFilterSelected = !state.filter.isAll)
        } else {
            if (!isHeightCompact() && !state.selectionEnabled) {
                Summary(state.summary)
            }

            if (state.anyMissingUpload && state.filter.isAll && !state.selectionEnabled) {
                UploadResults(onUploadClick = { onEvent(ResultsViewModel.Event.UploadClick) })
            }

            LazyColumn {
                state.results.forEach { (date, results) ->
                    stickyHeader(key = date.toString()) {
                        ResultDateHeader(date)
                    }
                    items(items = results) { result ->
                        val isSelected = result.isSelected
                        ResultCell(
                            item = result.item,
                            onResultClick = {
                                if (state.selectionEnabled) {
                                    onEvent(
                                        ResultsViewModel.Event.ToggleItemSelection(
                                            result.item,
                                            !isSelected,
                                        ),
                                    )
                                } else {
                                    onEvent(ResultsViewModel.Event.ResultClick(result.item))
                                }
                            },
                            isSelected = isSelected,
                            onSelectChange = { checked ->
                                onEvent(
                                    ResultsViewModel.Event.ToggleItemSelection(
                                        result.item,
                                        checked,
                                    ),
                                )
                            },
                            onLongClick = {
                                if (!isSelected) {
                                    onEvent(
                                        ResultsViewModel.Event.ToggleItemSelection(
                                            result.item,
                                            true,
                                        ),
                                    )
                                }
                            },
                        )
                        HorizontalDivider(thickness = with(LocalDensity.current) { 1.toDp() })
                    }
                }
                if (state.areResultsLimited) {
                    item("limited") {
                        Text(
                            text = stringResource(
                                Res.string.Results_LimitedNotice,
                                state.filter.limit,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 24.dp),
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        DeleteConfirmDialog(
            message = if (state.selectionEnabled) {
                pluralStringResource(
                    Res.plurals.Modal_DoYouWantToDeleteSomeTests,
                    state.selectedResultsCount,
                    state.selectedResultsCount,
                )
            } else if (state.filter.isAll) {
                stringResource(Res.string.Modal_DoYouWantToDeleteAllTests)
            } else {
                stringResource(Res.string.TestResults_Filter_DeleteConfirmation)
            },
            onConfirm = {
                onEvent(ResultsViewModel.Event.DeleteClick)
                showDeleteConfirm = false
            },
            onDismiss = {
                showDeleteConfirm = false
            },
        )
    }

    if (showMarkAsViewedConfirm) {
        MarkAllAsViewedConfirmDialog(
            filter = state.filter,
            onConfirm = {
                onEvent(ResultsViewModel.Event.MarkAsViewedClick)
                showMarkAsViewedConfirm = false
            },
            onDismiss = {
                showMarkAsViewedConfirm = false
            },
        )
    }

    LaunchedEffect(Unit) {
        onEvent(ResultsViewModel.Event.Start)
    }
}

@Composable
fun UploadResults(onUploadClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 2.dp,
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                stringResource(Res.string.Snackbar_ResultsSomeNotUploaded_Text),
                modifier = Modifier.weight(1f),
            )
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
private fun EmptyResults(anyFilterSelected: Boolean) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp)
            .alpha(0.5f),
    ) {
        Icon(
            painterResource(Res.drawable.ooni_empty_state),
            contentDescription = null,
        )
        Text(
            stringResource(
                if (anyFilterSelected) {
                    Res.string.TestResults_Filter_NoTestsFound
                } else {
                    Res.string.TestResults_Overview_NoTestsHaveBeenRun
                },
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun Summary(summary: ResultsViewModel.Summary?) {
    if (summary == null) return
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min) // So VerticalDividers don't expand to the whole screen
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(Res.string.TestResults_Overview_Hero_Tests),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    summary.resultsCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            VerticalDivider(Modifier.padding(4.dp), color = LocalContentColor.current)

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(Res.string.TestResults_Overview_Hero_Networks),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Text(
                    summary.networksCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            VerticalDivider(Modifier.padding(4.dp), color = LocalContentColor.current)

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(Res.string.TestResults_Overview_Hero_DataUsage),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_download),
                        contentDescription = stringResource(Res.string.TestResults_Summary_Performance_Hero_Download),
                        modifier = Modifier.size(16.dp).padding(end = 4.dp),
                    )
                    Text(summary.dataUsageDown.formatDataUsage())
                }
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_upload),
                        contentDescription = stringResource(Res.string.TestResults_Summary_Performance_Hero_Upload),
                        modifier = Modifier.size(16.dp).padding(end = 4.dp),
                    )
                    Text(summary.dataUsageUp.formatDataUsage())
                }
            }
        }
    }
}

@Composable
private fun ResultDateHeader(date: LocalDate) {
    val monthNames = stringMonthArrayResource()
    Text(
        date.format(
            Format {
                monthName(MonthNames(monthNames))
                char(' ')
                year()
            },
        ),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    message: String,
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        text = {
            Text(message)
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

@Composable
private fun MarkAllAsViewedConfirmDialog(
    filter: ResultFilter,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        text = {
            Text(
                stringResource(
                    if (filter.isAll) {
                        Res.string.Results_MarkAllAsViewed_Confirmation
                    } else {
                        Res.string.Results_MarkAllAsViewed_Filtered_Confirmation
                    },
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text(stringResource(Res.string.Common_Yes))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(Res.string.Modal_Cancel))
            }
        },
    )
}
