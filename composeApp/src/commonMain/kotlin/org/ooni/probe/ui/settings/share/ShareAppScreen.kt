package org.ooni.probe.ui.settings.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_ShareApp
import ooniprobe.composeapp.generated.resources.Settings_ShareApp_Description
import ooniprobe.composeapp.generated.resources.Settings_ShareApp_ShareMessage
import ooniprobe.composeapp.generated.resources.app_name
import ooniprobe.composeapp.generated.resources.logo_probe
import ooniprobe.composeapp.generated.resources.ooni_install_qrcode
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.PlatformAction
import org.ooni.probe.ui.shared.NavigationBackButton
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.shared.isHeightCompact
import org.ooni.probe.ui.theme.AppTheme

@Composable
fun ShareAppScreen(
    onBack: () -> Unit,
    launchAction: (PlatformAction) -> Boolean,
) {
    val coroutineScope = rememberCoroutineScope()

    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        TopBar(
            title = { Text(stringResource(Res.string.Settings_ShareApp)) },
            navigationIcon = { NavigationBackButton(onBack) },
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isHeightCompact()) 4.dp else 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            if (!isHeightCompact()) {
                Icon(
                    painter = painterResource(Res.drawable.logo_probe),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 32.dp, bottom = 16.dp),
                )
            }

            Text(
                stringResource(
                    Res.string.Settings_ShareApp_Description,
                    stringResource(Res.string.app_name),
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp),
            )

            Button(
                onClick = {
                    coroutineScope.launch {
                        launchAction(
                            PlatformAction.Share(
                                getString(
                                    Res.string.Settings_ShareApp_ShareMessage,
                                    getString(Res.string.app_name),
                                    OrganizationConfig.installUrl.orEmpty(),
                                ),
                            ),
                        )
                    }
                },
            ) {
                Text(
                    stringResource(Res.string.Settings_ShareApp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            Spacer(Modifier.weight(1f))

            Image(
                painterResource(Res.drawable.ooni_install_qrcode),
                contentDescription = null,
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .size(if (isHeightCompact()) 80.dp else 184.dp),
            )
            Text(
                OrganizationConfig.installUrl.orEmpty(),
                style = MaterialTheme.typography.labelLarge,
            )

            Spacer(Modifier.weight(2f))
        }
    }
}

@Composable
@Preview
fun SupportScreenPreview() {
    AppTheme {
        ShareAppScreen(
            onBack = {},
            launchAction = { false },
        )
    }
}
