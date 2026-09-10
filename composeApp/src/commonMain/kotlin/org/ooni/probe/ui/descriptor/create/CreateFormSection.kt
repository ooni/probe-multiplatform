package org.ooni.probe.ui.descriptor.create

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.CreateDescriptor_AddTest
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Description
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Inputs
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Inputs_Placeholder
import ooniprobe.composeapp.generated.resources.CreateDescriptor_LoggedInAs
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Name
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Options
import ooniprobe.composeapp.generated.resources.CreateDescriptor_RemoveTest
import ooniprobe.composeapp.generated.resources.CreateDescriptor_ShortDescription
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Submit
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Tests
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.ic_add
import ooniprobe.composeapp.generated.resources.ic_delete
import ooniprobe.composeapp.generated.resources.ic_keyboard_arrow_down
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TestType
import org.ooni.probe.ui.descriptor.create.CreateDescriptorViewModel.ErrorMessage
import org.ooni.probe.ui.descriptor.create.CreateDescriptorViewModel.Event
import org.ooni.probe.ui.descriptor.create.CreateDescriptorViewModel.NetTestForm
import org.ooni.probe.ui.descriptor.create.CreateDescriptorViewModel.Phase

@Composable
internal fun CreateForm(
    state: CreateDescriptorViewModel.State,
    onEvent: (Event) -> Unit,
) {
    state.session?.let { session ->
        Text(
            stringResource(Res.string.CreateDescriptor_LoggedInAs, session.emailAddress),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }

    OutlinedTextField(
        value = state.name,
        onValueChange = { onEvent(Event.NameChanged(it)) },
        label = { Text(stringResource(Res.string.CreateDescriptor_Name)) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("CreateDescriptor-Name"),
    )
    OutlinedTextField(
        value = state.shortDescription,
        onValueChange = { onEvent(Event.ShortDescriptionChanged(it)) },
        label = { Text(stringResource(Res.string.CreateDescriptor_ShortDescription)) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("CreateDescriptor-ShortDescription"),
    )
    OutlinedTextField(
        value = state.description,
        onValueChange = { onEvent(Event.DescriptionChanged(it)) },
        label = { Text(stringResource(Res.string.CreateDescriptor_Description)) },
        minLines = 3,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("CreateDescriptor-Description"),
    )

    Text(
        stringResource(Res.string.CreateDescriptor_Tests),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 24.dp),
    )

    state.netTests.forEachIndexed { index, netTest ->
        NetTestItem(
            index = index,
            netTest = netTest,
            canRemove = state.canRemoveNetTests,
            onEvent = onEvent,
        )
    }

    TextButton(
        onClick = { onEvent(Event.AddNetTestClicked) },
        modifier = Modifier
            .padding(top = 8.dp)
            .testTag("CreateDescriptor-AddTest"),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_add),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(18.dp),
        )
        Text(stringResource(Res.string.CreateDescriptor_AddTest))
    }

    ErrorText(state.errorMessage)

    Button(
        onClick = { onEvent(Event.SubmitClicked) },
        enabled = state.phase != Phase.Submitting,
        modifier = Modifier
            .padding(top = 24.dp)
            .testTag("CreateDescriptor-Submit"),
    ) {
        if (state.phase == Phase.Submitting) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(18.dp),
            )
        }
        Text(stringResource(Res.string.CreateDescriptor_Submit))
    }
}

@Composable
private fun NetTestItem(
    index: Int,
    netTest: NetTestForm,
    canRemove: Boolean,
    onEvent: (Event) -> Unit,
) {
    Column(Modifier.padding(top = 8.dp)) {
        HorizontalDivider(Modifier.padding(bottom = 8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            TestTypeDropdown(
                index = index,
                selected = netTest.test,
                onEvent = onEvent,
                modifier = Modifier.weight(1f),
            )
            if (canRemove) {
                IconButton(onClick = { onEvent(Event.RemoveNetTestClicked(index)) }) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_delete),
                        contentDescription = stringResource(Res.string.CreateDescriptor_RemoveTest),
                    )
                }
            }
        }

        OutlinedTextField(
            value = netTest.inputs,
            onValueChange = { onEvent(Event.NetTestInputsChanged(index, it)) },
            label = { Text(stringResource(Res.string.CreateDescriptor_Inputs)) },
            placeholder = { Text(stringResource(Res.string.CreateDescriptor_Inputs_Placeholder)) },
            minLines = 2,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Uri),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag("CreateDescriptor-Inputs-$index"),
        )
        OutlinedTextField(
            value = netTest.options,
            onValueChange = { onEvent(Event.NetTestOptionsChanged(index, it)) },
            label = { Text(stringResource(Res.string.CreateDescriptor_Options)) },
            isError = netTest.hasInvalidOptions,
            minLines = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag("CreateDescriptor-Options-$index"),
        )
        if (netTest.hasInvalidOptions) {
            ErrorText(ErrorMessage.InvalidOptions)
        }
    }
}

@Composable
private fun TestTypeDropdown(
    index: Int,
    selected: TestType,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("CreateDescriptor-TestType-$index"),
        ) {
            Text(
                text = selected.displayName,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(Res.drawable.ic_keyboard_arrow_down),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            TestType.ALL_NAMED.forEach { testType ->
                DropdownMenuItem(
                    text = { Text(testType.displayName) },
                    onClick = {
                        onEvent(Event.NetTestTypeChanged(index, testType))
                        expanded = false
                    },
                )
            }
        }
    }
}
