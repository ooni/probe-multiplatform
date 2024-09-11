package org.ooni.probe.ui.results

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.task_origin_all
import ooniprobe.composeapp.generated.resources.task_origin_auto_run
import ooniprobe.composeapp.generated.resources.task_origin_manual
import ooniprobe.composeapp.generated.resources.test_type_all
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.ResultFilter

@Composable
fun DescriptorFilter(
    current: ResultFilter.Type<Descriptor>,
    list: List<ResultFilter.Type<Descriptor>>,
    onFilterChanged: (ResultFilter.Type<Descriptor>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        CustomFilterChip(
            text = current.label(),
            selected = current != ResultFilter.Type.All,
            onClick = { expanded = true },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            list.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label()) },
                    onClick = {
                        onFilterChanged(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
fun OriginFilter(
    current: ResultFilter.Type<TaskOrigin>,
    list: List<ResultFilter.Type<TaskOrigin>>,
    onFilterChanged: (ResultFilter.Type<TaskOrigin>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        CustomFilterChip(
            text = current.name(),
            selected = current != ResultFilter.Type.All,
            onClick = { expanded = true },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            list.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name()) },
                    onClick = {
                        onFilterChanged(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun ExposedDropdownMenuBoxScope.CustomFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = { onClick() },
        label = {
            Text(
                text,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .menuAnchor(),
    )
}

@Composable
private fun ResultFilter.Type<Descriptor>.label() =
    when (this) {
        ResultFilter.Type.All -> stringResource(Res.string.test_type_all)
        is ResultFilter.Type.One -> value.title()
    }

@Composable
private fun ResultFilter.Type<TaskOrigin>.name() =
    when (this) {
        ResultFilter.Type.All -> stringResource(Res.string.task_origin_all)
        is ResultFilter.Type.One -> when (value) {
            TaskOrigin.AutoRun -> stringResource(Res.string.task_origin_auto_run)
            TaskOrigin.OoniRun -> stringResource(Res.string.task_origin_manual)
        }
    }
