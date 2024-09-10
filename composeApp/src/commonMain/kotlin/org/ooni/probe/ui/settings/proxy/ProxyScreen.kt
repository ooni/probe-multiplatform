package org.ooni.probe.ui.settings.proxy

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_Hostname
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_Port
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_Protocol
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Enabled
import ooniprobe.composeapp.generated.resources.back
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.ProxyProtocol
import org.ooni.probe.data.models.ProxyType

@Composable
fun ProxyScreen(
    state: ProxyViewModel.State,
    onEvent: (ProxyViewModel.Event) -> Unit,
) {
    Column {
        TopAppBar(
            title = {
                Text(stringResource(Res.string.Settings_Proxy_Enabled))
            },
            navigationIcon = {
                IconButton(onClick = {
                    onEvent(ProxyViewModel.Event.BackClicked)
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                    )
                }
            },
        )
        Column(Modifier.selectableGroup()) {
            ProxyType.entries.forEach { text ->
                Row(
                    Modifier.fillMaxWidth().height(56.dp).selectable(
                        selected = (text == state.proxyType),
                        onClick = {
                            onEvent(
                                ProxyViewModel.Event.ProtocolTypeSelected(
                                    protocol = text,
                                ),
                            )
                        },
                        role = Role.RadioButton,
                    ).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = (text == state.proxyType),
                        onClick = null,
                    )
                    Text(
                        text = stringResource(text.label),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }
        if (state.proxyType == ProxyType.CUSTOM) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                var expanded by remember { mutableStateOf(false) }
                var hostname by rememberSaveable { mutableStateOf(state.proxyHost ?: "") }
                var port by rememberSaveable { mutableStateOf(state.proxyPort?.toString() ?: "") }
                val protocols =
                    listOf(ProxyProtocol.HTTP, ProxyProtocol.HTTPS, ProxyProtocol.SOCKS5)
                OutlinedTextField(
                    value = state.proxyProtocol.toCustomProtocol(),
                    onValueChange = { },
                    label = {
                        Text(
                            stringResource(Res.string.Settings_Proxy_Custom_Protocol),
                            maxLines = 1,
                        )
                    },
                    modifier = Modifier.weight(0.3f),
                    // Workaround to detect click on TextField: https://stackoverflow.com/a/70335041
                    interactionSource = remember { MutableInteractionSource() }
                        .also { interactionSource ->
                            LaunchedEffect(interactionSource) {
                                interactionSource.interactions.collect {
                                    if (it is PressInteraction.Release) {
                                        expanded = true
                                    }
                                }
                            }
                        },
                    readOnly = true,
                    singleLine = true,
                    isError = state.proxyProtocolError,
                    maxLines = 1,
                    trailingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = stringResource(Res.string.Settings_Proxy_Custom_Protocol),
                            )
                        }
                    },
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    protocols.forEach { protocol ->
                        DropdownMenuItem(text = { Text(protocol.protocol) }, onClick = {
                            expanded = false
                            onEvent(ProxyViewModel.Event.ProtocolChanged(protocol))
                        })
                    }
                }

                OutlinedTextField(
                    value = hostname,
                    onValueChange = {
                        hostname = it
                        onEvent(
                            ProxyViewModel.Event.ProxyHostChanged(
                                host = it,
                            ),
                        )
                    },
                    label = { Text(stringResource(Res.string.Settings_Proxy_Custom_Hostname)) },
                    modifier = Modifier.weight(0.4f),
                    singleLine = true,
                    isError = state.proxyHostError,
                )

                OutlinedTextField(
                    value = port,
                    onValueChange = {
                        port = it
                        onEvent(
                            ProxyViewModel.Event.ProxyPortChanged(
                                port = port,
                            ),
                        )
                    },
                    label = { Text(stringResource(Res.string.Settings_Proxy_Custom_Port)) },
                    modifier = Modifier.weight(0.2f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = state.proxyPortError,
                )
            }
        }
    }
}
