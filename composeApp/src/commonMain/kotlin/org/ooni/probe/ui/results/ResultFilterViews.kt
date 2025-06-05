package org.ooni.probe.ui.results

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ooniprobe.composeapp.generated.resources.Common_Clear
import ooniprobe.composeapp.generated.resources.Common_Close
import ooniprobe.composeapp.generated.resources.Common_Save
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.TaskOrigin_AutoRun
import ooniprobe.composeapp.generated.resources.TaskOrigin_Manual
import ooniprobe.composeapp.generated.resources.TestResults_Filter_Sources
import ooniprobe.composeapp.generated.resources.TestResults_Filter_Tests
import ooniprobe.composeapp.generated.resources.TestResults_Filter_Tests_Multiple
import ooniprobe.composeapp.generated.resources.TestResults_Filters_Short
import ooniprobe.composeapp.generated.resources.TestResults_Filters_Title
import ooniprobe.composeapp.generated.resources.ic_check
import ooniprobe.composeapp.generated.resources.ic_close
import ooniprobe.composeapp.generated.resources.ic_tests
import ooniprobe.composeapp.generated.resources.ic_timer
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.ResultFilter
import org.ooni.probe.ui.shared.ellipsize

@Composable
fun ResultFiltersRow(
    filter: ResultFilter,
    onOpen: () -> Unit,
) {
    if (filter.isAll) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .padding(horizontal = 16.dp),
    ) {
        Text(stringResource(Res.string.TestResults_Filters_Short))

        FlowRow(
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            if (filter.descriptors.any()) {
                InputChip(
                    selected = true,
                    onClick = onOpen,
                    label = {
                        Text(
                            if (filter.descriptors.size == 1) {
                                filter.descriptors.first().title().ellipsize(20)
                            } else {
                                stringResource(
                                    Res.string.TestResults_Filter_Tests_Multiple,
                                    filter.descriptors.size,
                                    filter.descriptors.size,
                                )
                            },
                        )
                    },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            if (filter.taskOrigin != null) {
                InputChip(
                    selected = true,
                    onClick = onOpen,
                    label = { Text(filter.taskOrigin.name()) },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
fun ResultFiltersDialog(
    initialFilter: ResultFilter,
    descriptors: List<Descriptor>,
    onSave: (ResultFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var currentFilter by remember { mutableStateOf(initialFilter) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                TopAppBar(
                    title = { Text(stringResource(Res.string.TestResults_Filters_Title)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                painterResource(Res.drawable.ic_close),
                                contentDescription = stringResource(Res.string.Common_Close),
                            )
                        }
                    },
                    actions = {
                        if (!currentFilter.isAll) {
                            TextButton(onClick = { currentFilter = ResultFilter() }) {
                                Text(stringResource(Res.string.Common_Clear))
                            }
                        }
                        TextButton(onClick = { onSave(currentFilter) }) {
                            Text(stringResource(Res.string.Common_Save))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors().copy(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )

                ResultsFiltersDialogContent(
                    currentFilter,
                    { currentFilter = it },
                    descriptors,
                )
            }
        }
    }
}

@Composable
private fun ResultsFiltersDialogContent(
    filter: ResultFilter,
    updateFilter: (ResultFilter) -> Unit,
    descriptors: List<Descriptor>,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
    ) {
        FilterTitle(
            stringResource(Res.string.TestResults_Filter_Tests),
            painterResource(Res.drawable.ic_tests),
        )

        FlowRow(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp),
        ) {
            descriptors.forEach { descriptor ->
                val isSelected = filter.descriptors.contains(descriptor)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        updateFilter(
                            if (isSelected) {
                                filter.copy(descriptors = filter.descriptors - descriptor)
                            } else {
                                filter.copy(descriptors = filter.descriptors + descriptor)
                            },
                        )
                    },
                    label = { Text(descriptor.title().ellipsize(20)) },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(
                                painterResource(Res.drawable.ic_check),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        FilterTitle(
            stringResource(Res.string.TestResults_Filter_Sources),
            painterResource(Res.drawable.ic_timer),
        )

        FlowRow(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp),
        ) {
            TaskOrigin.entries.forEach { origin ->
                val isSelected = filter.taskOrigin == origin
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) {
                            updateFilter(filter.copy(taskOrigin = null))
                        } else {
                            updateFilter(filter.copy(taskOrigin = origin))
                        }
                    },
                    label = { Text(origin.name()) },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(
                                painterResource(Res.drawable.ic_check),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun FilterTitle(
    text: String,
    icon: Painter,
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun TaskOrigin.name() =
    when (this) {
        TaskOrigin.AutoRun -> stringResource(Res.string.TaskOrigin_AutoRun)
        TaskOrigin.OoniRun -> stringResource(Res.string.TaskOrigin_Manual)
    }
