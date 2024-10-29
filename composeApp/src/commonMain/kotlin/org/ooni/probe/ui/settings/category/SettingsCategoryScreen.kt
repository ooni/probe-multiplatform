package org.ooni.probe.ui.settings.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Modal_OK
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.back
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.PreferenceItemType
import org.ooni.probe.data.models.SettingsCategoryItem
import org.ooni.probe.data.models.SettingsKey
import org.ooni.probe.ui.shared.TopBar

@Composable
fun SettingsCategoryScreen(
    state: SettingsCategoryViewModel.State,
    onEvent: (SettingsCategoryViewModel.Event) -> Unit,
) {
    val category = state.category ?: return

    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        TopBar(
            title = {
                Text(stringResource(category.title))
            },
            navigationIcon = {
                IconButton(onClick = { onEvent(SettingsCategoryViewModel.Event.BackClicked) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                    )
                }
            },
        )
        Box(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(WindowInsets.navigationBars.asPaddingValues()),
        ) {
            Column {
                category.settings?.forEach { preferenceItem ->
                    when (preferenceItem.type) {
                        PreferenceItemType.SWITCH ->
                            SwitchSettingsView(
                                icon = preferenceItem.icon,
                                title = preferenceItem.title,
                                key = preferenceItem.key,
                                checked = state.preferences[preferenceItem.key] == true,
                                enabled = preferenceItem.enabled,
                                supportingContent = preferenceItem.supportingContent,
                                onCheckedChange = { key, value ->
                                    onEvent(
                                        SettingsCategoryViewModel.Event.CheckedChangeClick(
                                            key,
                                            value,
                                        ),
                                    )
                                },
                            )

                        PreferenceItemType.INT ->
                            NumberPickerItem(
                                title = preferenceItem.title,
                                supportingContent = preferenceItem.supportingContent,
                                enabled = preferenceItem.enabled,
                                value = state.preferences[preferenceItem.key] as? Int,
                                onChanged = {
                                    onEvent(
                                        SettingsCategoryViewModel.Event.IntChanged(
                                            preferenceItem.key,
                                            it,
                                        ),
                                    )
                                },
                            )

                        PreferenceItemType.BUTTON ->
                            RouteSettingsView(
                                title = preferenceItem.title,
                                supportingContent = preferenceItem.supportingContent,
                                trailingContent = preferenceItem.trailingContent,
                            )

                        PreferenceItemType.ROUTE ->
                            RouteSettingsView(
                                title = preferenceItem.title,
                                supportingContent = preferenceItem.supportingContent,
                                modifier =
                                    Modifier.clickable {
                                        if (preferenceItem is SettingsCategoryItem) {
                                            onEvent(
                                                SettingsCategoryViewModel.Event.SettingsCategoryClick(
                                                    preferenceItem.route,
                                                ),
                                            )
                                        }
                                    },
                            )

                        PreferenceItemType.SELECT ->
                            RouteSettingsView(
                                title = preferenceItem.title,
                                supportingContent = preferenceItem.supportingContent,
                            )
                    }
                }
                category.footerContent?.let {
                    HorizontalDivider(Modifier.padding(vertical = 16.dp))
                    it.invoke()
                }
            }
        }
    }
}

@Composable
fun SwitchSettingsView(
    title: StringResource,
    icon: DrawableResource?,
    supportingContent: @Composable (() -> Unit)? = null,
    key: SettingsKey,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (SettingsKey, Boolean) -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(stringResource(title))
        },
        supportingContent = supportingContent,
        leadingContent = icon?.let {
            {
                Icon(
                    modifier = Modifier.height(24.dp).width(24.dp),
                    painter = painterResource(it),
                    contentDescription = stringResource(title),
                )
            }
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = { newValue -> onCheckedChange(key, newValue) },
                enabled = enabled,
            )
        },
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.5f)
            .run {
                if (enabled) {
                    clickable { onCheckedChange(key, !checked) }
                } else {
                    this
                }
            },
    )
}

@Composable
fun RouteSettingsView(
    title: StringResource,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        modifier = modifier,
    )
}

@Composable
fun SettingsDescription(description: StringResource) {
    Text(
        stringResource(description),
        modifier = Modifier.padding(horizontal = 16.dp),
        fontSize = 12.sp,
    )
}

@Composable
fun NumberPickerItem(
    title: StringResource,
    supportingContent: @Composable (() -> Unit)? = null,
    enabled: Boolean,
    value: Int?,
    onChanged: (Int?) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = supportingContent,
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.5f)
            .run {
                if (enabled) {
                    clickable { showDialog = true }
                } else {
                    this
                }
            },
    )

    if (showDialog) {
        var fieldValue by remember { mutableStateOf(value?.toString() ?: "") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(title)) },
            text = {
                OutlinedTextField(
                    value = fieldValue,
                    onValueChange = { newValue ->
                        fieldValue = newValue.replace(Regex("[^0-9]"), "")
                        isError = false
                    },
                    isError = isError,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                    ),
                    modifier = Modifier.testTag("NumberPickerField"),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val intValue = fieldValue.toIntOrNull() ?: run {
                            isError = true
                            return@TextButton
                        }
                        if (intValue <= 0) {
                            isError = true
                            return@TextButton
                        }

                        onChanged(intValue)
                        showDialog = false
                    },
                ) {
                    Text(stringResource(Res.string.Modal_OK))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(Res.string.Modal_Cancel))
                }
            },
        )
    }
}
