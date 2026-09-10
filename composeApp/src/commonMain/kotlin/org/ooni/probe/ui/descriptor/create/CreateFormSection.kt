package org.ooni.probe.ui.descriptor.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import ooniprobe.composeapp.generated.resources.CreateDescriptor_AddUrl
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Description
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Expiration
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Icon
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Icon_Choose
import ooniprobe.composeapp.generated.resources.CreateDescriptor_LoggedInAs
import ooniprobe.composeapp.generated.resources.CreateDescriptor_LogOut
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Name
import ooniprobe.composeapp.generated.resources.CreateDescriptor_RemoveUrl
import ooniprobe.composeapp.generated.resources.CreateDescriptor_ShortDescription
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Submit
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Urls
import ooniprobe.composeapp.generated.resources.CreateDescriptor_Urls_Placeholder
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Modal_OK
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.ic_add
import ooniprobe.composeapp.generated.resources.ic_cancel
import ooniprobe.composeapp.generated.resources.ooni_empty_state
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.shared.InstalledDescriptorIcons
import org.ooni.probe.shared.toEpochInUTC
import org.ooni.probe.shared.toLocalDateFromUtc
import org.ooni.probe.shared.today
import org.ooni.probe.ui.descriptor.create.CreateDescriptorViewModel.Event
import org.ooni.probe.ui.descriptor.create.CreateDescriptorViewModel.State
import org.ooni.probe.ui.descriptor.create.CreateDescriptorViewModel.UrlItem

@Composable
internal fun CreateForm(
    state: State.LoggedIn,
    onEvent: (Event) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        Text(
            stringResource(Res.string.CreateDescriptor_LoggedInAs, state.session.emailAddress),
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(
            onClick = { onEvent(Event.LogOutClicked) },
            modifier = Modifier
                .padding(start = 4.dp)
                .testTag("CreateDescriptor-LogOut"),
        ) {
            Text(
                stringResource(Res.string.CreateDescriptor_LogOut),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
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

    IconField(
        selectedIcon = state.icon,
        onEvent = onEvent,
    )

    ExpirationDateField(
        expirationDate = state.expirationDate,
        onEvent = onEvent,
    )

    Text(
        stringResource(Res.string.CreateDescriptor_Urls),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 24.dp),
    )

    state.urls.forEachIndexed { index, item ->
        UrlField(
            index = index,
            item = item,
            canRemove = state.canRemoveUrls,
            onEvent = onEvent,
        )
    }

    TextButton(
        onClick = { onEvent(Event.AddUrlClicked) },
        enabled = state.canAddUrls,
        modifier = Modifier
            .padding(top = 8.dp)
            .testTag("CreateDescriptor-AddUrl"),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_add),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(18.dp),
        )
        Text(stringResource(Res.string.CreateDescriptor_AddUrl))
    }

    ErrorText(state.errorMessage)

    Button(
        onClick = { onEvent(Event.SubmitClicked) },
        enabled = !state.isSubmitting,
        modifier = Modifier
            .padding(top = 24.dp)
            .testTag("CreateDescriptor-Submit"),
    ) {
        if (state.isSubmitting) {
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
private fun UrlField(
    index: Int,
    item: UrlItem,
    canRemove: Boolean,
    onEvent: (Event) -> Unit,
) {
    OutlinedTextField(
        value = item.url,
        onValueChange = { onEvent(Event.UrlChanged(index, it)) },
        placeholder = { Text(stringResource(Res.string.CreateDescriptor_Urls_Placeholder)) },
        isError = item.hasError,
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Uri),
        trailingIcon = {
            if (canRemove) {
                IconButton(onClick = { onEvent(Event.DeleteUrlClicked(index)) }) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_cancel),
                        contentDescription = stringResource(Res.string.CreateDescriptor_RemoveUrl),
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("CreateDescriptor-Url-$index"),
    )
}

@Composable
private fun IconField(
    selectedIcon: String?,
    onEvent: (Event) -> Unit,
) {
    var showIconPicker by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showIconPicker = true },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("CreateDescriptor-Icon"),
    ) {
        Icon(
            painter = painterResource(
                selectedIcon?.let { InstalledDescriptorIcons.getIconFromValue(it) }
                    ?: Res.drawable.ooni_empty_state,
            ),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(20.dp),
        )
        Text(stringResource(Res.string.CreateDescriptor_Icon))
    }

    if (!showIconPicker) return

    AlertDialog(
        onDismissRequest = { showIconPicker = false },
        title = { Text(stringResource(Res.string.CreateDescriptor_Icon_Choose)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconOption(
                        icon = Res.drawable.ooni_empty_state,
                        isSelected = selectedIcon == null,
                        contentDescription = null,
                        testTag = "CreateDescriptor-Icon-None",
                        onClick = {
                            onEvent(Event.IconChanged(null))
                            showIconPicker = false
                        },
                    )
                    InstalledDescriptorIcons.entries.forEach { entry ->
                        IconOption(
                            icon = entry.icon,
                            isSelected = selectedIcon == entry.value,
                            contentDescription = entry.value,
                            testTag = "CreateDescriptor-Icon-${entry.value}",
                            onClick = {
                                onEvent(Event.IconChanged(entry.value))
                                showIconPicker = false
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { showIconPicker = false }) {
                Text(stringResource(Res.string.Modal_Cancel))
            }
        },
    )
}

@Composable
private fun IconOption(
    icon: DrawableResource,
    isSelected: Boolean,
    contentDescription: String?,
    testTag: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .background(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    Color.Transparent
                },
                shape = CircleShape,
            ).testTag(testTag),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun ExpirationDateField(
    expirationDate: LocalDate,
    onEvent: (Event) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showDatePicker = true },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("CreateDescriptor-Expiration"),
    ) {
        Text(stringResource(Res.string.CreateDescriptor_Expiration, expirationDate.toString()))
    }

    if (!showDatePicker) return

    val today = LocalDate.today()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = expirationDate.toEpochInUTC(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis.toLocalDateFromUtc() > today

            override fun isSelectableYear(year: Int) = year >= today.year
        },
    )

    DatePickerDialog(
        onDismissRequest = { showDatePicker = false },
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onEvent(Event.ExpirationDateChanged(it.toLocalDateFromUtc()))
                    }
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
        DatePicker(state = datePickerState, showModeToggle = false)
    }
}
