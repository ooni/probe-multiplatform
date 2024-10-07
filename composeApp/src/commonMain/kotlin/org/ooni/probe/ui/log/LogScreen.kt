package org.ooni.probe.ui.log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Severity
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Storage_Delete
import ooniprobe.composeapp.generated.resources.back
import ooniprobe.composeapp.generated.resources.filter_logs
import ooniprobe.composeapp.generated.resources.ic_delete_all
import ooniprobe.composeapp.generated.resources.logs
import ooniprobe.composeapp.generated.resources.share_logs
import ooniprobe.composeapp.generated.resources.share_logs_error
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.LocalSnackbarHostState
import org.ooni.probe.ui.shared.ColorDefaults
import org.ooni.probe.ui.shared.CustomFilterChip
import org.ooni.probe.ui.theme.LocalCustomColors

@Composable
fun LogScreen(
    state: LogViewModel.State,
    onEvent: (LogViewModel.Event) -> Unit,
) {
    Column {
        TopAppBar(
            title = { Text(stringResource(Res.string.logs)) },
            navigationIcon = {
                IconButton(onClick = { onEvent(LogViewModel.Event.BackClicked) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                    )
                }
            },
            actions = {
                IconButton(onClick = { onEvent(LogViewModel.Event.ClearClicked) }) {
                    Icon(
                        painterResource(Res.drawable.ic_delete_all),
                        contentDescription = stringResource(Res.string.Settings_Storage_Delete),
                    )
                }
                IconButton(onClick = { onEvent(LogViewModel.Event.ShareClicked) }) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = stringResource(Res.string.share_logs),
                    )
                }
            },
            colors = ColorDefaults.topAppBar(),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
        ) {
            Text(
                stringResource(Res.string.filter_logs),
                modifier = Modifier.weight(2f),
            )
            SeverityFilter(
                current = state.filter,
                onFilterChanged = { onEvent(LogViewModel.Event.FilterChanged(it)) },
                modifier = Modifier.weight(1f),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
        ) {
            items(state.log) { line ->
                Text(
                    line,
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        line.contains(": WARN : ") -> LocalCustomColors.current.logWarn
                        line.contains(": ERROR : ") -> LocalCustomColors.current.logError
                        line.contains(": INFO : ") -> LocalCustomColors.current.logInfo
                        else -> LocalCustomColors.current.logDebug
                    },
                )
            }
        }
    }

    val snackbarHostState = LocalSnackbarHostState.current ?: return
    LaunchedEffect(state.errors) {
        val error = state.errors.firstOrNull() ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            getString(
                when (error) {
                    LogViewModel.Error.Share -> Res.string.share_logs_error
                },
            ),
        )
        onEvent(LogViewModel.Event.ErrorShown(error))
    }
}

@Composable
fun SeverityFilter(
    current: Severity?,
    onFilterChanged: (Severity?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.width(IntrinsicSize.Min),
    ) {
        CustomFilterChip(
            text = current.label(),
            selected = current != null,
            onClick = { expanded = true },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SEVERITY_OPTIONS.forEach { option ->
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

private fun Severity?.label() = this?.name?.uppercase() ?: "ALL"

private val SEVERITY_OPTIONS = listOf(
    null,
    Severity.Error,
    Severity.Warn,
    Severity.Info,
    Severity.Debug,
)
