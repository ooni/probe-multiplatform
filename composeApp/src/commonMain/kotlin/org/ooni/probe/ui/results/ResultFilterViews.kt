package org.ooni.probe.ui.results

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
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
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Modal_OK
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.TaskOrigin_AutoRun
import ooniprobe.composeapp.generated.resources.TaskOrigin_Manual
import ooniprobe.composeapp.generated.resources.TestResults_Filter_Date
import ooniprobe.composeapp.generated.resources.TestResults_Filter_Date_Any
import ooniprobe.composeapp.generated.resources.TestResults_Filter_Date_Custom
import ooniprobe.composeapp.generated.resources.TestResults_Filter_Date_Custom_Filled
import ooniprobe.composeapp.generated.resources.TestResults_Filter_Date_FromOneMonthAgo
import ooniprobe.composeapp.generated.resources.TestResults_Filter_Date_FromSevenDaysAgo
import ooniprobe.composeapp.generated.resources.TestResults_Filter_Date_Picker
import ooniprobe.composeapp.generated.resources.TestResults_Filter_Date_Today
import ooniprobe.composeapp.generated.resources.TestResults_Filter_Networks
import ooniprobe.composeapp.generated.resources.TestResults_Filter_Networks_Count
import ooniprobe.composeapp.generated.resources.TestResults_Filter_Source
import ooniprobe.composeapp.generated.resources.TestResults_Filter_Tests
import ooniprobe.composeapp.generated.resources.TestResults_Filter_Tests_Count
import ooniprobe.composeapp.generated.resources.TestResults_Filters_Count
import ooniprobe.composeapp.generated.resources.TestResults_Filters_Short
import ooniprobe.composeapp.generated.resources.TestResults_Filters_Title
import ooniprobe.composeapp.generated.resources.TestResults_UnknownASN
import ooniprobe.composeapp.generated.resources.ic_check
import ooniprobe.composeapp.generated.resources.ic_close
import ooniprobe.composeapp.generated.resources.ic_date_range
import ooniprobe.composeapp.generated.resources.ic_network
import ooniprobe.composeapp.generated.resources.ic_tests
import ooniprobe.composeapp.generated.resources.ic_timer
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.NetworkModel
import org.ooni.probe.data.models.ResultFilter
import org.ooni.probe.shared.toEpochInUTC
import org.ooni.probe.shared.toLocalDateFromUtc
import org.ooni.probe.ui.shared.ellipsize
import org.ooni.probe.ui.shared.isoFormat

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
            .padding(start = 16.dp, end = 4.dp),
    ) {
        Text(stringResource(Res.string.TestResults_Filters_Short))

        FlowRow(
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            if (filter.filterCount > 2) {
                InputChip(
                    selected = true,
                    onClick = onOpen,
                    label = {
                        Text(
                            pluralStringResource(
                                Res.plurals.TestResults_Filters_Count,
                                filter.filterCount,
                                filter.filterCount,
                            ),
                        )
                    },
                    modifier = Modifier.padding(start = 8.dp),
                )
                return@FlowRow
            }

            if (filter.descriptors.any()) {
                InputChip(
                    selected = true,
                    onClick = onOpen,
                    label = {
                        Text(
                            if (filter.descriptors.size == 1) {
                                filter.descriptors.first().title().ellipsize(20)
                            } else {
                                pluralStringResource(
                                    Res.plurals.TestResults_Filter_Tests_Count,
                                    filter.descriptors.size,
                                    filter.descriptors.size,
                                )
                            },
                        )
                    },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            if (filter.networks.any()) {
                InputChip(
                    selected = true,
                    onClick = onOpen,
                    label = {
                        Text(
                            if (filter.networks.size == 1) {
                                filter.networks.first().name()
                            } else {
                                pluralStringResource(
                                    Res.plurals.TestResults_Filter_Networks_Count,
                                    filter.networks.size,
                                    filter.networks.size,
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

            if (filter.dates != ResultFilter.Date.AnyDate) {
                InputChip(
                    selected = true,
                    onClick = onOpen,
                    label = { Text(filter.dates.display()) },
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
    networks: List<NetworkModel>,
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
                    },
                    colors = TopAppBarDefaults.topAppBarColors().copy(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    ResultsFiltersDialogContent(
                        currentFilter,
                        { currentFilter = it },
                        descriptors,
                        networks,
                    )

                    Button(
                        onClick = { onSave(currentFilter) },
                        enabled = initialFilter != currentFilter,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .padding(WindowInsets.navigationBars.asPaddingValues()),
                    ) {
                        Text(
                            stringResource(Res.string.Common_Save),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsFiltersDialogContent(
    filter: ResultFilter,
    updateFilter: (ResultFilter) -> Unit,
    descriptors: List<Descriptor>,
    networks: List<NetworkModel>,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
            // So content can scroll above the Save button
            .padding(bottom = 64.dp),
    ) {
        DateFilter(filter, updateFilter)
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        OriginFilter(filter, updateFilter)
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        TestsFilter(filter, updateFilter, descriptors)
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        NetworksFilter(filter, updateFilter, networks)
    }
}

@Composable
private fun TestsFilter(
    filter: ResultFilter,
    updateFilter: (ResultFilter) -> Unit,
    descriptors: List<Descriptor>,
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
            ResultFilterChip(
                text = descriptor.title().ellipsize(20),
                isSelected = isSelected,
                onClick = {
                    updateFilter(
                        if (isSelected) {
                            filter.copy(descriptors = filter.descriptors - descriptor)
                        } else {
                            filter.copy(descriptors = filter.descriptors + descriptor)
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun OriginFilter(
    filter: ResultFilter,
    updateFilter: (ResultFilter) -> Unit,
) {
    FilterTitle(
        stringResource(Res.string.TestResults_Filter_Source),
        painterResource(Res.drawable.ic_timer),
    )

    FlowRow(
        modifier = Modifier.padding(start = 16.dp, end = 8.dp),
    ) {
        TaskOrigin.entries.forEach { origin ->
            val isSelected = filter.taskOrigin == origin
            ResultFilterChip(
                text = origin.name(),
                isSelected = isSelected,
                onClick = {
                    updateFilter(
                        if (isSelected) {
                            filter.copy(taskOrigin = null)
                        } else {
                            filter.copy(taskOrigin = origin)
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun TaskOrigin.name() =
    when (this) {
        TaskOrigin.AutoRun -> stringResource(Res.string.TaskOrigin_AutoRun)
        TaskOrigin.OoniRun -> stringResource(Res.string.TaskOrigin_Manual)
    }

@Composable
private fun DateFilter(
    filter: ResultFilter,
    updateFilter: (ResultFilter) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    FilterTitle(
        stringResource(Res.string.TestResults_Filter_Date),
        painterResource(Res.drawable.ic_date_range),
    )

    FlowRow(
        modifier = Modifier.padding(start = 16.dp, end = 8.dp),
    ) {
        listOf(
            ResultFilter.Date.AnyDate,
            ResultFilter.Date.Today,
            ResultFilter.Date.FromSevenDaysAgo,
            ResultFilter.Date.FromOneMonthAgo,
        ).forEach { dateFilter ->
            ResultFilterChip(
                text = dateFilter.display(),
                isSelected = filter.dates == dateFilter,
                onClick = { updateFilter(filter.copy(dates = dateFilter)) },
            )
        }

        val customDate = filter.dates as? ResultFilter.Date.Custom

        ResultFilterChip(
            text = customDate?.let {
                stringResource(Res.string.TestResults_Filter_Date_Custom_Filled, it.display())
            } ?: stringResource(Res.string.TestResults_Filter_Date_Custom),
            isSelected = customDate != null,
            onClick = { showDatePicker = true },
        )
    }

    if (showDatePicker) {
        val initialRange = (filter.dates as? ResultFilter.Date.Custom)?.customRange
        val selectableRange = ResultFilter.Date.AnyDate.range
        val selectableYearRange = selectableRange.let { it.start.year..it.endInclusive.year }

        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = initialRange?.start?.toEpochInUTC(),
            initialSelectedEndDateMillis = initialRange?.endInclusive?.toEpochInUTC(),
            yearRange = selectableYearRange,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = selectableRange.contains(utcTimeMillis.toLocalDateFromUtc())

                override fun isSelectableYear(year: Int) = selectableYearRange.contains(year)
            },
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start =
                            dateRangePickerState.selectedStartDateMillis?.toLocalDateFromUtc()
                                ?: return@TextButton
                        val end =
                            dateRangePickerState.selectedEndDateMillis?.toLocalDateFromUtc()
                                ?: return@TextButton
                        updateFilter(
                            filter.copy(dates = ResultFilter.Date.Custom(start..end)),
                        )
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(Res.string.Modal_OK))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(Res.string.Modal_Cancel))
                }
            },
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        stringResource(Res.string.TestResults_Filter_Date_Picker),
                        style = MaterialTheme.typography.titleSmall,
                    )
                },
                showModeToggle = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun ResultFilter.Date.display() =
    when (this) {
        ResultFilter.Date.Today ->
            stringResource(Res.string.TestResults_Filter_Date_Today)

        ResultFilter.Date.FromSevenDaysAgo ->
            stringResource(Res.string.TestResults_Filter_Date_FromSevenDaysAgo)

        ResultFilter.Date.FromOneMonthAgo ->
            stringResource(Res.string.TestResults_Filter_Date_FromOneMonthAgo)

        ResultFilter.Date.AnyDate ->
            stringResource(Res.string.TestResults_Filter_Date_Any)

        is ResultFilter.Date.Custom ->
            if (customRange.start == customRange.endInclusive) {
                customRange.start.isoFormat()
            } else {
                customRange.start.isoFormat() + " – " + customRange.endInclusive.isoFormat()
            }
    }

@Composable
private fun NetworksFilter(
    filter: ResultFilter,
    updateFilter: (ResultFilter) -> Unit,
    networks: List<NetworkModel>,
) {
    FilterTitle(
        stringResource(Res.string.TestResults_Filter_Networks),
        painterResource(Res.drawable.ic_network),
    )

    FlowRow(
        modifier = Modifier.padding(start = 16.dp, end = 8.dp),
    ) {
        networks.forEach { network ->
            val isSelected = filter.networks.contains(network)
            ResultFilterChip(
                text = network.name(),
                isSelected = isSelected,
                onClick = {
                    updateFilter(
                        if (isSelected) {
                            filter.copy(networks = filter.networks - network)
                        } else {
                            filter.copy(networks = filter.networks + network)
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun NetworkModel.name(): String {
    val name = networkName?.ellipsize(20)
        ?: asn
        ?: stringResource(Res.string.TestResults_UnknownASN)
    return name + countryCode?.let { " ($it)" }
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
private fun ResultFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text) },
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
