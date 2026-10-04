package org.ooni.probe.ui.settings.proxy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Common_Save
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_Add
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_Authentication
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_HidePassword
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_Hostname
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_HostnameInvalid
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_Password
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_PasswordInvalid
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_Port
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_PortInvalid
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_Protocol
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_ShowPassword
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_Username
import ooniprobe.composeapp.generated.resources.Settings_Proxy_Custom_UsernameInvalid
import ooniprobe.composeapp.generated.resources.ic_visibility
import ooniprobe.composeapp.generated.resources.ic_visibility_off
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.CustomProxyProtocol
import org.ooni.probe.ui.shared.NavigationBackButton
import org.ooni.probe.ui.shared.TopBar

@Composable
fun AddProxyScreen(
    state: AddProxyViewModel.State,
    onEvent: (AddProxyViewModel.Event) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        TopBar(
            title = {
                Text(stringResource(Res.string.Settings_Proxy_Custom_Add))
            },
            navigationIcon = {
                NavigationBackButton({ onEvent(AddProxyViewModel.Event.BackClicked) })
            },
            actions = {
                TextButton(
                    onClick = { onEvent(AddProxyViewModel.Event.SaveClicked) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Text(stringResource(Res.string.Common_Save))
                }
            },
        )

        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .padding(WindowInsets.ime.asPaddingValues()),
        ) {
            Column(
                Modifier
                    .selectableGroup()
                    .padding(bottom = 24.dp)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    stringResource(Res.string.Settings_Proxy_Custom_Protocol),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                CustomProxyProtocol.entries.forEach { protocol ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .selectable(
                                selected = state.protocol == protocol,
                                onClick = {
                                    onEvent(AddProxyViewModel.Event.ProtocolChanged(protocol))
                                },
                                role = Role.RadioButton,
                            ).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = state.protocol == protocol,
                            onClick = null,
                        )
                        Text(
                            text = protocol.value.uppercase(),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }

            Column(
                Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
            ) {
                OutlinedTextField(
                    value = state.host,
                    onValueChange = { onEvent(AddProxyViewModel.Event.HostChanged(it)) },
                    label = { Text(stringResource(Res.string.Settings_Proxy_Custom_Hostname)) },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Uri,
                    ),
                    maxLines = 1,
                    isError = state.showHostAsInvalid,
                    supportingText = {
                        if (state.showHostAsInvalid) {
                            Text(stringResource(Res.string.Settings_Proxy_Custom_HostnameInvalid))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("AddProxy-HostField"),
                )

                OutlinedTextField(
                    value = state.port,
                    onValueChange = { onEvent(AddProxyViewModel.Event.PortChanged(it)) },
                    label = { Text(stringResource(Res.string.Settings_Proxy_Custom_Port)) },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                    ),
                    maxLines = 1,
                    isError = state.showPortAsInvalid,
                    supportingText = {
                        if (state.showPortAsInvalid) {
                            Text(stringResource(Res.string.Settings_Proxy_Custom_PortInvalid))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("AddProxy-PortField"),
                )

                Text(
                    text = stringResource(Res.string.Settings_Proxy_Custom_Authentication),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                )

                OutlinedTextField(
                    value = state.username,
                    onValueChange = { onEvent(AddProxyViewModel.Event.UsernameChanged(it)) },
                    label = { Text(stringResource(Res.string.Settings_Proxy_Custom_Username)) },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Text,
                    ),
                    maxLines = 1,
                    isError = state.showUsernameAsInvalid,
                    supportingText = {
                        if (state.showUsernameAsInvalid) {
                            Text(stringResource(Res.string.Settings_Proxy_Custom_UsernameInvalid))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("AddProxy-UsernameField"),
                )

                OutlinedTextField(
                    value = state.password,
                    onValueChange = { onEvent(AddProxyViewModel.Event.PasswordChanged(it)) },
                    label = { Text(stringResource(Res.string.Settings_Proxy_Custom_Password)) },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Password,
                    ),
                    visualTransformation = if (state.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { onEvent(AddProxyViewModel.Event.TogglePasswordVisibility) },
                            modifier = Modifier.testTag("AddProxy-TogglePasswordVisibility"),
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (state.isPasswordVisible) {
                                        Res.drawable.ic_visibility_off
                                    } else {
                                        Res.drawable.ic_visibility
                                    },
                                ),
                                contentDescription = stringResource(
                                    if (state.isPasswordVisible) {
                                        Res.string.Settings_Proxy_Custom_HidePassword
                                    } else {
                                        Res.string.Settings_Proxy_Custom_ShowPassword
                                    },
                                ),
                            )
                        }
                    },
                    maxLines = 1,
                    isError = state.showPasswordAsInvalid,
                    supportingText = {
                        if (state.showPasswordAsInvalid) {
                            Text(stringResource(Res.string.Settings_Proxy_Custom_PasswordInvalid))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("AddProxy-PasswordField"),
                )
            }
        }
    }
}
