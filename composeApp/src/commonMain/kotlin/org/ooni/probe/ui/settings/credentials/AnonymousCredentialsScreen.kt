package org.ooni.probe.ui.settings.credentials

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Measurements_Verification_Failed
import ooniprobe.composeapp.generated.resources.Measurements_Verification_Unverified
import ooniprobe.composeapp.generated.resources.Measurements_Verification_Verified
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_AnonymousCredentials_Description
import ooniprobe.composeapp.generated.resources.Settings_AnonymousCredentials_Label
import ooniprobe.composeapp.generated.resources.Settings_AnonymousCredentials_ProbeId
import ooniprobe.composeapp.generated.resources.Settings_AnonymousCredentials_ProbeIdUnavailable
import ooniprobe.composeapp.generated.resources.Settings_AnonymousCredentials_Reset
import ooniprobe.composeapp.generated.resources.Settings_AnonymousCredentials_Reset_Confirmation
import ooniprobe.composeapp.generated.resources.Settings_AnonymousCredentials_Reset_Title
import ooniprobe.composeapp.generated.resources.Settings_AnonymousCredentials_Status_CredentialNeedsReset
import ooniprobe.composeapp.generated.resources.Settings_AnonymousCredentials_Status_Loading
import ooniprobe.composeapp.generated.resources.Settings_AnonymousCredentials_Status_NoCredential
import ooniprobe.composeapp.generated.resources.Settings_AnonymousCredentials_Status_Ready
import ooniprobe.composeapp.generated.resources.Settings_AnonymousCredentials_Status_StoredWithoutNetwork
import ooniprobe.composeapp.generated.resources.Settings_AnonymousCredentials_Verification_Description
import ooniprobe.composeapp.generated.resources.Settings_AnonymousCredentials_Verification_Failed
import ooniprobe.composeapp.generated.resources.Settings_AnonymousCredentials_Verification_Title
import ooniprobe.composeapp.generated.resources.Settings_AnonymousCredentials_Verification_Unknown
import ooniprobe.composeapp.generated.resources.Settings_AnonymousCredentials_Verification_Unverified
import ooniprobe.composeapp.generated.resources.Settings_AnonymousCredentials_Verification_Verified
import ooniprobe.composeapp.generated.resources.ic_shield
import ooniprobe.composeapp.generated.resources.ic_shield_check
import ooniprobe.composeapp.generated.resources.ic_shield_warning
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.passport.models.VerificationStatus
import org.ooni.probe.domain.credentials.AnonymousCredentialsHealth
import org.ooni.probe.ui.shared.NavigationBackButton
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.theme.LocalCustomColors

@Composable
fun AnonymousCredentialsScreen(
    state: AnonymousCredentialsViewModel.State,
    onEvent: (AnonymousCredentialsViewModel.Event) -> Unit,
) {
    var showResetConfirmation by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("AnonymousCredentialsScreen"),
    ) {
        TopBar(
            title = { Text(stringResource(Res.string.Settings_AnonymousCredentials_Label)) },
            navigationIcon = {
                NavigationBackButton({ onEvent(AnonymousCredentialsViewModel.Event.BackClicked) })
            },
        )

        val scrollState = rememberScrollState()
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(WindowInsets.navigationBars.asPaddingValues()),
        ) {
            Text(
                stringResource(Res.string.Settings_AnonymousCredentials_Description),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 20.dp, bottom = 16.dp),
            )

            CredentialHealthSection(state)

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            VerificationStatesSection()

            Button(
                onClick = { showResetConfirmation = true },
                enabled = !state.isResetting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 24.dp)
                    .testTag("AnonymousCredentials-Reset"),
            ) {
                if (state.isResetting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onError,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp),
                    )
                }
                Text(stringResource(Res.string.Settings_AnonymousCredentials_Reset))
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text(stringResource(Res.string.Settings_AnonymousCredentials_Reset_Title)) },
            text = { Text(stringResource(Res.string.Settings_AnonymousCredentials_Reset_Confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(AnonymousCredentialsViewModel.Event.ResetConfirmed)
                        showResetConfirmation = false
                    },
                    modifier = Modifier.testTag("AnonymousCredentials-ConfirmReset"),
                ) {
                    Text(stringResource(Res.string.Settings_AnonymousCredentials_Reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text(stringResource(Res.string.Modal_Cancel))
                }
            },
        )
    }
}

@Composable
private fun CredentialHealthSection(state: AnonymousCredentialsViewModel.State) {
    val health = state.health
    val status = when (health) {
        null -> stringResource(Res.string.Settings_AnonymousCredentials_Status_Loading)
        AnonymousCredentialsHealth.NoCredential ->
            stringResource(Res.string.Settings_AnonymousCredentials_Status_NoCredential)
        AnonymousCredentialsHealth.StoredCredentialWithoutNetwork ->
            stringResource(Res.string.Settings_AnonymousCredentials_Status_StoredWithoutNetwork)
        AnonymousCredentialsHealth.CredentialNeedsReset ->
            stringResource(Res.string.Settings_AnonymousCredentials_Status_CredentialNeedsReset)
        is AnonymousCredentialsHealth.Ready ->
            stringResource(Res.string.Settings_AnonymousCredentials_Status_Ready)
    }
    val statusColor = when (health) {
        is AnonymousCredentialsHealth.Ready -> LocalCustomColors.current.success
        AnonymousCredentialsHealth.CredentialNeedsReset -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        CredentialHealthIcon(health, status, statusColor)
        Text(
            text = status,
            color = statusColor,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
    }

    ListItem(
        headlineContent = { Text(stringResource(Res.string.Settings_AnonymousCredentials_ProbeId)) },
        supportingContent = {
            when (health) {
                is AnonymousCredentialsHealth.Ready -> {
                    SelectionContainer {
                        Text(
                            text = health.probeId,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text = "${health.probeCc} · ${health.probeAsn}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                else -> Text(
                    text = stringResource(Res.string.Settings_AnonymousCredentials_ProbeIdUnavailable),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        colors = androidx.compose.material3.ListItemDefaults.colors(
            containerColor = Color.Transparent,
        ),
        modifier = Modifier.testTag("AnonymousCredentials-ProbeId"),
    )
}

@Composable
private fun VerificationStatesSection() {
    Text(
        stringResource(Res.string.Settings_AnonymousCredentials_Verification_Title),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
    )
    Text(
        stringResource(Res.string.Settings_AnonymousCredentials_Verification_Description),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
    )

    VerificationStateText(
        verificationStatus = VerificationStatus.Verified,
        resource = Res.string.Settings_AnonymousCredentials_Verification_Verified,
    )
    VerificationStateText(
        verificationStatus = VerificationStatus.Unverified,
        resource = Res.string.Settings_AnonymousCredentials_Verification_Unverified,
    )
    VerificationStateText(
        verificationStatus = VerificationStatus.Failed,
        resource = Res.string.Settings_AnonymousCredentials_Verification_Failed,
    )
    VerificationStateText(
        verificationStatus = VerificationStatus.Unknown,
        resource = Res.string.Settings_AnonymousCredentials_Verification_Unknown,
    )
}

@Composable
private fun CredentialHealthIcon(
    health: AnonymousCredentialsHealth?,
    contentDescription: String,
    tint: Color,
) {
    when (health) {
        null -> CircularProgressIndicator(
            color = tint,
            strokeWidth = 2.dp,
            modifier = Modifier.padding(end = 8.dp).size(16.dp),
        )

        else -> Icon(
            painter = painterResource(
                when (health) {
                    is AnonymousCredentialsHealth.Ready -> Res.drawable.ic_shield_check
                    AnonymousCredentialsHealth.CredentialNeedsReset -> Res.drawable.ic_shield_warning
                    else -> Res.drawable.ic_shield
                },
            ),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.padding(end = 8.dp).size(20.dp),
        )
    }
}

@Composable
private fun VerificationStateText(
    verificationStatus: VerificationStatus,
    resource: org.jetbrains.compose.resources.StringResource,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(top = 6.dp),
    ) {
        VerificationStateIcon(verificationStatus)
        Text(
            stringResource(resource),
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 20.sp),
        )
    }
}

@Composable
private fun VerificationStateIcon(verificationStatus: VerificationStatus) {
    val icon: DrawableResource
    val contentDescription: String
    val tint: Color
    when (verificationStatus) {
        VerificationStatus.Verified -> {
            icon = Res.drawable.ic_shield_check
            contentDescription = stringResource(Res.string.Measurements_Verification_Verified)
            tint = LocalCustomColors.current.success
        }

        VerificationStatus.Failed -> {
            icon = Res.drawable.ic_shield_warning
            contentDescription = stringResource(Res.string.Measurements_Verification_Failed)
            tint = MaterialTheme.colorScheme.error
        }

        else -> {
            icon = Res.drawable.ic_shield
            contentDescription = stringResource(
                if (verificationStatus == VerificationStatus.Unknown) {
                    Res.string.Settings_AnonymousCredentials_Verification_Unknown
                } else {
                    Res.string.Measurements_Verification_Unverified
                },
            )
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
    Icon(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.padding(end = 8.dp).size(16.dp),
    )
}
