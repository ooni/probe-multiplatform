package org.ooni.probe.ui.settings.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ooniprobe.composeapp.generated.resources.Common_Back
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Support_Action
import ooniprobe.composeapp.generated.resources.Settings_Support_IncludeLogs
import ooniprobe.composeapp.generated.resources.Settings_Support_Label
import ooniprobe.composeapp.generated.resources.Settings_Support_Message
import ooniprobe.composeapp.generated.resources.Settings_Support_Message_Hint
import ooniprobe.composeapp.generated.resources.Settings_Support_SendEmail
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.domain.SendSupportEmail
import org.ooni.probe.ui.shared.TopBar

@Composable
fun SupportScreen(
    onBack: () -> Unit,
    sendSupportEmail: suspend (SendSupportEmail.Params) -> Boolean,
) {
    val coroutineScope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var includeLogs by remember { mutableStateOf(false) }

    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        TopBar(
            title = {
                Text(stringResource(Res.string.Settings_Support_Label))
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.Common_Back),
                    )
                }
            },
        )
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Text(
                stringResource(Res.string.Settings_Support_SendEmail),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 4.dp),
            )

            Text(
                stringResource(Res.string.Settings_Support_Message),
                modifier = Modifier.padding(bottom = 16.dp),
            )

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(Res.string.Settings_Support_Message_Hint)) },
                minLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .toggleable(
                        value = includeLogs,
                        onValueChange = { includeLogs = it },
                        role = Role.Switch,
                    )
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    stringResource(Res.string.Settings_Support_IncludeLogs),
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = includeLogs,
                    onCheckedChange = null,
                )
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        val success = sendSupportEmail(SendSupportEmail.Params(text, includeLogs))
                        if (success) {
                            onBack()
                        }
                    }
                },
                enabled = text.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 16.dp)
                    .defaultMinSize(minHeight = 48.dp),
            ) {
                Text(
                    stringResource(Res.string.Settings_Support_Action),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(end = 16.dp),
                )

                Icon(Icons.AutoMirrored.Default.Send, contentDescription = null)
            }
        }
    }
}
