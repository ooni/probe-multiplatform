package org.ooni.probe.ui.choosewebsites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Common_Back
import ooniprobe.composeapp.generated.resources.Common_Clear
import ooniprobe.composeapp.generated.resources.CustomWebsites_Fab_Text
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Modal_CustomURL_NotSaved
import ooniprobe.composeapp.generated.resources.Modal_CustomURL_Title_NotSaved
import ooniprobe.composeapp.generated.resources.Modal_Delete
import ooniprobe.composeapp.generated.resources.Modal_OK
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Websites_CustomURL_Add
import ooniprobe.composeapp.generated.resources.Settings_Websites_CustomURL_Title
import ooniprobe.composeapp.generated.resources.Settings_Websites_CustomURL_URL
import ooniprobe.composeapp.generated.resources.ic_add
import ooniprobe.composeapp.generated.resources.ic_cancel
import ooniprobe.composeapp.generated.resources.ic_timer
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.theme.AppTheme

@Composable
fun ChooseWebsitesScreen(
    state: ChooseWebsitesViewModel.State,
    onEvent: (ChooseWebsitesViewModel.Event) -> Unit,
) {
    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
    ) {
        TopBar(
            title = { Text(stringResource(Res.string.Settings_Websites_CustomURL_Title)) },
            navigationIcon = {
                IconButton(onClick = { onEvent(ChooseWebsitesViewModel.Event.BackClicked) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.Common_Back),
                    )
                }
            },
            actions = {
                if (state.canClearUrls) {
                    TextButton(
                        onClick = { onEvent(ChooseWebsitesViewModel.Event.ClearClicked) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        Text(stringResource(Res.string.Common_Clear))
                    }
                }
            },
        )

        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.imePadding()
                    .testTag("ChooseWebsites-List"),
                contentPadding = PaddingValues(
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + 64.dp,
                ),
            ) {
                itemsIndexed(state.websites) { index, item ->
                    OutlinedTextField(
                        value = item.url,
                        onValueChange = {
                            onEvent(
                                ChooseWebsitesViewModel.Event.UrlChanged(
                                    index,
                                    it,
                                ),
                            )
                        },
                        label = { stringResource(Res.string.Settings_Websites_CustomURL_URL) },
                        isError = item.hasError,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Uri,
                        ),
                        trailingIcon = {
                            if (state.canRemoveUrls) {
                                IconButton(
                                    onClick = {
                                        onEvent(
                                            ChooseWebsitesViewModel.Event.DeleteWebsiteClicked(
                                                index,
                                            ),
                                        )
                                    },
                                ) {
                                    Icon(
                                        painterResource(Res.drawable.ic_cancel),
                                        contentDescription = stringResource(Res.string.Modal_Delete),
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp)
                            .testTag("ChooseWebsites-UrlField"),
                    )
                }

                item(key = "Add") {
                    TextButton(
                        onClick = { onEvent(ChooseWebsitesViewModel.Event.AddWebsiteClicked) },
                        enabled = state.canAddUrls,
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                        Text(stringResource(Res.string.Settings_Websites_CustomURL_Add))
                    }
                }
            }

            Button(
                onClick = { onEvent(ChooseWebsitesViewModel.Event.RunClicked) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            ) {
                Icon(
                    painterResource(Res.drawable.ic_timer),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    pluralStringResource(
                        Res.plurals.CustomWebsites_Fab_Text,
                        state.websites.size,
                        state.websites.size,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }

    if (state.showBackConfirmation) {
        BackConfirmationDialog(
            onConfirm = { onEvent(ChooseWebsitesViewModel.Event.BackConfirmed) },
            onDismiss = { onEvent(ChooseWebsitesViewModel.Event.BackCancelled) },
        )
    }
}

@Composable
private fun BackConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(Res.string.Modal_CustomURL_Title_NotSaved)) },
        text = { Text(stringResource(Res.string.Modal_CustomURL_NotSaved)) },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text(stringResource(Res.string.Modal_OK))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(Res.string.Modal_Cancel))
            }
        },
    )
}

@Preview
@Composable
fun ChooseWebsitesScreenPreview() {
    AppTheme {
        ChooseWebsitesScreen(
            state = ChooseWebsitesViewModel.State(),
            onEvent = {},
        )
    }
}
