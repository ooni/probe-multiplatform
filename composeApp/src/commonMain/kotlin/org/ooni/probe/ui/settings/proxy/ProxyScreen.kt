package org.ooni.probe.ui.settings.proxy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Modal_Delete
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Proxy
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_Add
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_Available
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_Testing
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_Unavailable
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Delete_Confirmation
import ooniprobe.composeapp.generated.resources.Settings_Proxy_None
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Psiphon
import ooniprobe.composeapp.generated.resources.ic_check
import ooniprobe.composeapp.generated.resources.ic_cloud_off
import ooniprobe.composeapp.generated.resources.ic_delete
import ooniprobe.composeapp.generated.resources.ic_download
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.ProxyOption
import org.ooni.probe.domain.proxy.TestProxy
import org.ooni.probe.ui.shared.NavigationBackButton
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.shared.VerticalScrollbar
import org.ooni.probe.ui.theme.LocalCustomColors

@Composable
fun ProxyScreen(
    state: ProxyViewModel.State,
    onEvent: (ProxyViewModel.Event) -> Unit,
) {
    var showDeleteConfirmationDialog by remember { mutableStateOf<ProxyOption.Custom?>(null) }

    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        TopBar(
            title = {
                Text(stringResource(Res.string.Settings_Proxy))
            },
            navigationIcon = {
                NavigationBackButton({ onEvent(ProxyViewModel.Event.BackClicked) })
            },
        )
        Box(Modifier.fillMaxSize()) {
            val lazyListState = rememberLazyListState()
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.selectableGroup(),
            ) {
                items(state.items, { it.option.value }) { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 60.dp)
                            .selectable(
                                selected = item.isSelected,
                                onClick = {
                                    onEvent(ProxyViewModel.Event.OptionSelected(item.option))
                                },
                                role = Role.RadioButton,
                            ).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = item.isSelected,
                            onClick = null,
                        )
                        Column(
                            modifier = Modifier.padding(start = 16.dp).weight(1f),
                        ) {
                            Text(
                                text = when (item.option) {
                                    ProxyOption.None -> stringResource(Res.string.Settings_Proxy_None)
                                    ProxyOption.Psiphon -> stringResource(Res.string.Settings_Proxy_Psiphon)
                                    is ProxyOption.Custom -> item.option.value
                                },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            item.testState?.let {
                                TestProxyStateText(it)
                            }
                        }
                        if (item.option is ProxyOption.Custom) {
                            IconButton(
                                onClick = { showDeleteConfirmationDialog = item.option },
                                modifier = Modifier.alpha(0.66f),
                            ) {
                                Icon(
                                    painterResource(Res.drawable.ic_delete),
                                    contentDescription = stringResource(Res.string.Modal_Delete),
                                )
                            }
                        }
                    }
                    HorizontalDivider(thickness = Dp.Hairline)
                }
                item("add") {
                    TextButton(
                        onClick = { onEvent(ProxyViewModel.Event.AddCustomClicked) },
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 4.dp, end = 16.dp),
                            )
                            Text(
                                stringResource(Res.string.Settings_Proxy_Custom_Add),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
            VerticalScrollbar(state = lazyListState, modifier = Modifier.align(Alignment.CenterEnd))
        }
    }

    showDeleteConfirmationDialog?.let {
        DeleteConfirmationDialog(
            option = it,
            onConfirm = {
                onEvent(ProxyViewModel.Event.DeleteCustomClicked(it))
                showDeleteConfirmationDialog = null
            },
            onDismiss = { showDeleteConfirmationDialog = null },
        )
    }
}

@Composable
private fun TestProxyStateText(state: TestProxy.State) {
    val contentColor = when (state) {
        TestProxy.State.Testing -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.66f)
        TestProxy.State.Unavailable -> MaterialTheme.colorScheme.error
        TestProxy.State.Available -> LocalCustomColors.current.success
    }
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state == TestProxy.State.Testing) {
                CircularProgressIndicator(
                    color = LocalContentColor.current,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                Icon(
                    painter = painterResource(
                        when (state) {
                            TestProxy.State.Testing -> Res.drawable.ic_download
                            TestProxy.State.Unavailable -> Res.drawable.ic_cloud_off
                            TestProxy.State.Available -> Res.drawable.ic_check
                        },
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = stringResource(
                    when (state) {
                        TestProxy.State.Testing -> Res.string.Settings_Proxy_Custom_Testing
                        TestProxy.State.Unavailable -> Res.string.Settings_Proxy_Custom_Unavailable
                        TestProxy.State.Available -> Res.string.Settings_Proxy_Custom_Available
                    },
                ),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    option: ProxyOption.Custom,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        text = {
            Text(
                stringResource(
                    Res.string.Settings_Proxy_Delete_Confirmation,
                    option.value,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text(stringResource(Res.string.Modal_Delete))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(Res.string.Modal_Cancel))
            }
        },
    )
}
