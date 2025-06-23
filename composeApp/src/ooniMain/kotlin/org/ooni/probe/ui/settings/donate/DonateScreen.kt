package org.ooni.probe.ui.settings.donate

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ooniprobe.composeapp.generated.resources.Common_Back
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Settings_Donate_Action
import ooniprobe.composeapp.generated.resources.Settings_Donate_Action_Error
import ooniprobe.composeapp.generated.resources.Settings_Donate_Action_Warning_ExternalBrowser
import ooniprobe.composeapp.generated.resources.Settings_Donate_Heading
import ooniprobe.composeapp.generated.resources.Settings_Donate_SubHeading
import ooniprobe.composeapp.generated.resources.Settings_Donate_Support_Item_OpenData
import ooniprobe.composeapp.generated.resources.Settings_Donate_Support_Item_OpenSource
import ooniprobe.composeapp.generated.resources.Settings_Donate_Support_Item_Research
import ooniprobe.composeapp.generated.resources.Settings_Donate_Support_Title
import ooniprobe.composeapp.generated.resources.Settings_Donate_Title
import ooniprobe.composeapp.generated.resources.donate_octopus_1
import ooniprobe.composeapp.generated.resources.donate_octopus_2
import ooniprobe.composeapp.generated.resources.ic_heart
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.LocalSnackbarHostState
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.PreferenceCategoryKey
import org.ooni.probe.data.models.SettingsCategoryItem
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.theme.LocalCustomColors

val DONATE_SETTINGS_ITEM
    get() = SettingsCategoryItem(
        icon = Res.drawable.ic_heart,
        title = Res.string.Settings_Donate_Title,
        route = PreferenceCategoryKey.DONATE,
    )

@Composable
fun DonateScreen(
    onBack: () -> Unit,
    openUrl: (String) -> Boolean,
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current
    val cardBackgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)

    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        TopBar(
            title = { Text(stringResource(Res.string.Settings_Donate_Title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.Common_Back),
                    )
                }
            },
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            LocalCustomColors.current.donateBackground,
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                ),
        ) {
            Image(
                painterResource(Res.drawable.donate_octopus_1),
                contentDescription = null,
                alpha = 0.5f,
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(0.45f),
            )
            Image(
                painterResource(Res.drawable.donate_octopus_2),
                contentDescription = null,
                alpha = 0.5f,
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth(0.45f)
                    .align(Alignment.BottomEnd),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(WindowInsets.navigationBars.asPaddingValues())
                    .padding(bottom = 128.dp),
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 56.dp),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            stringResource(Res.string.Settings_Donate_Heading),
                            style = MaterialTheme.typography.titleLarge
                                .copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        Text(
                            stringResource(Res.string.Settings_Donate_SubHeading),
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = cardBackgroundColor,
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            stringResource(Res.string.Settings_Donate_Support_Title),
                            style = MaterialTheme.typography.bodyLarge
                                .copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        val bullet = "● "
                        Text(
                            bullet +
                                stringResource(Res.string.Settings_Donate_Support_Item_OpenSource),
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        Text(
                            bullet +
                                stringResource(Res.string.Settings_Donate_Support_Item_OpenData),
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        Text(
                            bullet +
                                stringResource(Res.string.Settings_Donate_Support_Item_Research),
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }

                ElevatedButton(
                    onClick = {
                        if (!openUrl(OrganizationConfig.donateUrl)) {
                            coroutineScope.launch {
                                snackbarHostState?.showSnackbar(
                                    getString(
                                        Res.string.Settings_Donate_Action_Error,
                                        OrganizationConfig.donateUrl,
                                    ),
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(),
                    modifier = Modifier.fillMaxWidth()
                        .defaultMinSize(minHeight = 88.dp)
                        .padding(horizontal = 16.dp)
                        .padding(vertical = 16.dp),
                ) {
                    Icon(
                        painterResource(Res.drawable.ic_heart),
                        contentDescription = null,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    Text(
                        stringResource(Res.string.Settings_Donate_Action),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                Text(
                    stringResource(Res.string.Settings_Donate_Action_Warning_ExternalBrowser),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 32.dp)
                        .background(cardBackgroundColor, RoundedCornerShape(32.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }
}
