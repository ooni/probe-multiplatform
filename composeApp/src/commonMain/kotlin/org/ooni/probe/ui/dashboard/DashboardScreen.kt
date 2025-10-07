package org.ooni.probe.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ooniprobe.composeapp.generated.resources.Common_Month
import ooniprobe.composeapp.generated.resources.Common_Today
import ooniprobe.composeapp.generated.resources.Common_Week
import ooniprobe.composeapp.generated.resources.Dashboard_AutoRun_Description
import ooniprobe.composeapp.generated.resources.Dashboard_AutoRun_Disabled
import ooniprobe.composeapp.generated.resources.Dashboard_AutoRun_Enabled
import ooniprobe.composeapp.generated.resources.Dashboard_AutoRun_Title
import ooniprobe.composeapp.generated.resources.Dashboard_Stats_Countries
import ooniprobe.composeapp.generated.resources.Dashboard_Stats_Empty
import ooniprobe.composeapp.generated.resources.Dashboard_Stats_Networks
import ooniprobe.composeapp.generated.resources.Dashboard_Stats_Title
import ooniprobe.composeapp.generated.resources.Modal_DisableVPN_Title
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.app_name
import ooniprobe.composeapp.generated.resources.dashboard_arc
import ooniprobe.composeapp.generated.resources.donate_octopus_1
import ooniprobe.composeapp.generated.resources.donate_octopus_2
import ooniprobe.composeapp.generated.resources.ic_auto_run
import ooniprobe.composeapp.generated.resources.ic_close
import ooniprobe.composeapp.generated.resources.ic_heart
import ooniprobe.composeapp.generated.resources.ic_history
import ooniprobe.composeapp.generated.resources.ic_measurement_anomaly
import ooniprobe.composeapp.generated.resources.ic_measurement_failed
import ooniprobe.composeapp.generated.resources.ic_settings
import ooniprobe.composeapp.generated.resources.ic_tests
import ooniprobe.composeapp.generated.resources.ic_warning
import ooniprobe.composeapp.generated.resources.ic_world
import ooniprobe.composeapp.generated.resources.logo_probe
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ooni.probe.data.models.MeasurementStats
import org.ooni.probe.ui.shared.IgnoreBatteryOptimizationDialog
import org.ooni.probe.ui.shared.TestRunErrorMessages
import org.ooni.probe.ui.shared.isHeightCompact
import org.ooni.probe.ui.theme.AppTheme
import org.ooni.probe.ui.theme.LocalCustomColors
import org.ooni.probe.ui.theme.customColors

@Composable
fun DashboardScreen(
    state: DashboardViewModel.State,
    onEvent: (DashboardViewModel.Event) -> Unit,
) {
    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        // Colorful top background
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(WindowInsets.statusBars.asPaddingValues())
                .height(if (isHeightCompact()) 64.dp else 112.dp),
        ) {
            Image(
                painterResource(Res.drawable.logo_probe),
                contentDescription = stringResource(Res.string.app_name),
                modifier = Modifier
                    .padding(vertical = if (isHeightCompact()) 4.dp else 20.dp)
                    .align(Alignment.Center)
                    .height(if (isHeightCompact()) 48.dp else 72.dp),
            )
        }

        Box {
            Image(
                painterResource(Res.drawable.dashboard_arc),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth().height(32.dp),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                RunBackgroundStateSection(state.runBackgroundState, onEvent)
            }
        }

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp),
        ) {
            // LastResultSection(onEvent)
            // LastResultCard()
            // LastResultCardVariant()
            // AutoRunSwitch()
            // AutoRunCard()

            // Spacer(Modifier.height(24.dp))
            // HorizontalDivider(thickness = Dp.Hairline, modifier = Modifier.padding(vertical = 24.dp))
            // AutoRunSwitch2(state.isAutoRunEnabled, onEvent)
            Spacer(Modifier.height(16.dp))
            // LastResultCard()
            TestsCard()
            // HorizontalDivider(thickness = Dp.Hairline, modifier = Modifier.padding(vertical = 24.dp),)
            Spacer(Modifier.height(24.dp))
            StatsSection2(state.measurementStats)
            HorizontalDivider(
                thickness = Dp.Hairline,
                modifier = Modifier.padding(vertical = 24.dp),
            )
            BlogSection()

            if (state.showVpnWarning) {
                VpnWarning()
            }
        }
    }

    TestRunErrorMessages(
        errors = state.testRunErrors,
        onErrorDisplayed = { onEvent(DashboardViewModel.Event.ErrorDisplayed(it)) },
    )

    if (state.showIgnoreBatteryOptimizationNotice) {
        IgnoreBatteryOptimizationDialog(
            onAccepted = { onEvent(DashboardViewModel.Event.IgnoreBatteryOptimizationAccepted) },
            onDismissed = { onEvent(DashboardViewModel.Event.IgnoreBatteryOptimizationDismissed) },
        )
    }

    LaunchedEffect(Unit) {
        onEvent(DashboardViewModel.Event.Start)
    }
}

@Composable
private fun StatsSection() {
    Text(
        "Your Measurements",
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
    )
    FlowRow(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp),
        ) {
            Text("125", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Today",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp),
        ) {
            Text("1.8K", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Week",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp),
        ) {
            Text("33K", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Month",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp),
        ) {
            Text("6", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Networks",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp),
        ) {
            Text("2", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Countries",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 32.dp),
    ) {
        Text("Thank you", style = MaterialTheme.typography.labelLarge)
        Icon(
            painterResource(Res.drawable.ic_heart),
            contentDescription = null,
            modifier = Modifier.padding(start = 4.dp).size(16.dp),
        )
    }
}

@Composable
private fun StatsSection2(stats: MeasurementStats?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Text(
            stringResource(Res.string.Dashboard_Stats_Title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 20.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Icon(
            painterResource(Res.drawable.ic_heart),
            contentDescription = null,
            modifier = Modifier.padding(start = 8.dp).size(16.dp),
        )
    }

    @Composable
    fun StatsEntry(
        key: String,
        value: Long?,
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp),
        ) {
            Text(
                value?.toString().orEmpty(),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 24.sp),
            )
            Text(
                key.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
            )
        }
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        if (stats?.isEmpty == true) {
            Text(stringResource(Res.string.Dashboard_Stats_Empty))
        } else {
            StatsEntry(stringResource(Res.string.Common_Today), stats?.measurementsToday)
            StatsEntry(stringResource(Res.string.Common_Week), stats?.measurementsWeek)
            StatsEntry(stringResource(Res.string.Common_Month), stats?.measurementsMonth)
            StatsEntry(
                pluralStringResource(
                    Res.plurals.Dashboard_Stats_Networks,
                    stats?.networks?.toInt() ?: 0,
                ),
                stats?.networks,
            )
            StatsEntry(
                pluralStringResource(
                    Res.plurals.Dashboard_Stats_Countries,
                    stats?.countries?.toInt() ?: 0,
                ),
                stats?.countries,
            )
        }
    }
}

@Composable
private fun ColumnScope.LastResultSection(onEvent: (DashboardViewModel.Event) -> Unit) {
    Text(
        "Last result",
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
        ),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        "1 hour ago",
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
    )

    FlowRow(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        AssistChip(
            onClick = {},
            label = { Text("32 tested") },
            leadingIcon = {
                Icon(
                    painterResource(Res.drawable.ic_world),
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            },
            modifier = Modifier.padding(end = 2.dp),
        )
        AssistChip(
            onClick = {},
            label = { Text("2 blocked") },
            leadingIcon = {
                Icon(
                    painterResource(Res.drawable.ic_measurement_anomaly),
                    contentDescription = null,
                    tint = LocalCustomColors.current.logWarn,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            },
            modifier = Modifier.padding(end = 2.dp),
        )
        AssistChip(
            onClick = {},
            label = { Text("1 failed") },
            leadingIcon = {
                Icon(
                    painterResource(Res.drawable.ic_measurement_failed),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            },
            modifier = Modifier.padding(end = 2.dp),
        )
    }
    Button(
        onClick = { onEvent(DashboardViewModel.Event.SeeResultsClick) },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.customColors.success,
            contentColor = MaterialTheme.customColors.onSuccess,
        ),
        modifier = Modifier.Companion.align(Alignment.CenterHorizontally),
    ) {
        Text("See results")
    }
}

@Composable
private fun AutoRunCard() {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.padding(16.dp),
    ) {
        Box {
            Icon(
                painterResource(Res.drawable.ic_auto_run),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                modifier = Modifier.size(96.dp).align(Alignment.TopEnd).padding(top = 8.dp),
            )
            Column {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 8.dp),
                ) {
                    Text(
                        "Auto-run is disabled",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    "Enable to run tests automatically every hour. This ensures the constant monitoring of state changes",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .padding(horizontal = 8.dp),
                ) {
                    TextButton(onClick = {}) {
                        Text(
                            "Dismiss",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {}) {
                        Text("Enable")
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoRunSwitch() {
    var autoRun by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Text(
                "Auto-Run",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                if (autoRun) "enabled" else "disabled",
                style = MaterialTheme.typography.labelMedium,
                color = if (autoRun) LocalCustomColors.current.success else Color.Gray,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp).padding(top = 8.dp),
            )
            IconButton(
                onClick = {},
            ) {
                Icon(painterResource(Res.drawable.ic_settings), contentDescription = null)
            }
            Switch(
                checked = autoRun,
                onCheckedChange = { autoRun = it },
                thumbContent = {
                    Icon(
                        painterResource(if (autoRun) Res.drawable.ic_auto_run else Res.drawable.ic_close),
                        contentDescription = null,
                        tint = if (autoRun) Color.Black else LocalContentColor.current,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                },
                modifier = Modifier.scale(1.3f).padding(start = 8.dp, end = 8.dp),
            )
        }

        Text(
            "Tests run automatically every hour. This ensures the constant monitoring of state changes.",
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun AutoRunSwitch2(
    isAutoRunEnabled: Boolean,
    onEvent: (DashboardViewModel.Event) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Text(
                stringResource(Res.string.Dashboard_AutoRun_Title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                stringResource(
                    if (isAutoRunEnabled) {
                        Res.string.Dashboard_AutoRun_Enabled
                    } else {
                        Res.string.Dashboard_AutoRun_Disabled
                    },
                ),
                style = MaterialTheme.typography.labelMedium,
                color = if (isAutoRunEnabled) LocalCustomColors.current.success else Color.Gray,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp).padding(top = 8.dp),
            )
        }

        Row {
            Text(
                stringResource(Res.string.Dashboard_AutoRun_Description),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = isAutoRunEnabled,
                onCheckedChange = { onEvent(DashboardViewModel.Event.AutoRunEnabledChanged(it)) },
                thumbContent = {
                    Icon(
                        painterResource(if (isAutoRunEnabled) Res.drawable.ic_auto_run else Res.drawable.ic_close),
                        contentDescription = null,
                        tint = if (isAutoRunEnabled) Color.Black else LocalContentColor.current,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                },
                modifier = Modifier.scale(1.25f).padding(start = 8.dp, end = 8.dp),
            )
        }
    }
}

@Composable
private fun LastResultCard() {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = {},
    ) {
        Box {
            Icon(
                painterResource(Res.drawable.ic_history),
                contentDescription = null,
                tint = LocalContentColor.current.copy(alpha = 0.1f),
                modifier = Modifier.size(96.dp).align(Alignment.TopEnd).padding(top = 8.dp),
            )
            Column {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 4.dp),
                ) {
                    Text(
                        "Last result",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        "1 hour ago",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
                    )
                }
                FlowRow(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text("32 tested") },
                        leadingIcon = {
                            Icon(
                                painterResource(Res.drawable.ic_world),
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                        modifier = Modifier.padding(end = 2.dp),
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text("2 blocked") },
                        leadingIcon = {
                            Icon(
                                painterResource(Res.drawable.ic_measurement_anomaly),
                                contentDescription = null,
                                tint = LocalCustomColors.current.logWarn,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                        modifier = Modifier.padding(end = 2.dp),
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text("1 failed") },
                        leadingIcon = {
                            Icon(
                                painterResource(Res.drawable.ic_measurement_failed),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                        modifier = Modifier.padding(end = 2.dp),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp)
                        .padding(horizontal = 8.dp),
                ) {
                    TextButton(onClick = {}) {
                        Text(
                            "Dismiss",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {}) {
                        Text("See results")
                    }
                }
            }
        }
    }
}

@Composable
private fun LastResultCardVariant() {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFFf1f8f1),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = {},
    ) {
        Box {
            Icon(
                painterResource(Res.drawable.ic_history),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(96.dp)
                    .align(Alignment.TopEnd)
                    .absoluteOffset(x = 24.dp, y = -16.dp),
            )
            Column {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 4.dp),
                ) {
                    Text(
                        "Last result",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        "1 hour ago",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
                    )
                }
                FlowRow(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text("32 tested") },
                        leadingIcon = {
                            Icon(
                                painterResource(Res.drawable.ic_world),
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                        modifier = Modifier.padding(end = 2.dp),
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text("2 blocked") },
                        leadingIcon = {
                            Icon(
                                painterResource(Res.drawable.ic_measurement_anomaly),
                                contentDescription = null,
                                tint = LocalCustomColors.current.logWarn,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                        modifier = Modifier.padding(end = 2.dp),
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text("1 failed") },
                        leadingIcon = {
                            Icon(
                                painterResource(Res.drawable.ic_measurement_failed),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                        modifier = Modifier.padding(end = 2.dp),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp)
                        .padding(horizontal = 8.dp),
                ) {
                    TextButton(onClick = {}) {
                        Text(
                            "Dismiss",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {}) {
                        Text("See results")
                    }
                }
            }
        }
    }
}

@Composable
private fun LastResultCardPrimary() {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = {},
    ) {
        Box {
            Icon(
                painterResource(Res.drawable.ic_history),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.33f),
                modifier = Modifier
                    .size(96.dp)
                    .align(Alignment.TopEnd)
                    .absoluteOffset(x = 24.dp, y = -16.dp),
            )
            Column {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 4.dp),
                ) {
                    Text(
                        "Last result",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        "1 hour ago",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
                    )
                }
                FlowRow(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text("32 tested") },
                        leadingIcon = {
                            Icon(
                                painterResource(Res.drawable.ic_world),
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = LocalContentColor.current,
                            leadingIconContentColor = LocalContentColor.current,
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = LocalContentColor.current,
                        ),
                        modifier = Modifier.padding(end = 2.dp),
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text("2 blocked") },
                        leadingIcon = {
                            Icon(
                                painterResource(Res.drawable.ic_measurement_anomaly),
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = LocalContentColor.current,
                            leadingIconContentColor = LocalContentColor.current,
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = LocalContentColor.current,
                        ),
                        modifier = Modifier.padding(end = 2.dp),
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text("1 failed") },
                        leadingIcon = {
                            Icon(
                                painterResource(Res.drawable.ic_measurement_failed),
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            labelColor = LocalContentColor.current,
                            leadingIconContentColor = LocalContentColor.current,
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = LocalContentColor.current,
                        ),
                        modifier = Modifier.padding(end = 2.dp),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp)
                        .padding(horizontal = 8.dp),
                ) {
                    TextButton(onClick = {}) {
                        Text(
                            "Dismiss",
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.66f),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {}) {
                        Text("See results", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

@Composable
private fun TestsCard() {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = {},
    ) {
        Box {
            Icon(
                painterResource(Res.drawable.ic_tests),
                contentDescription = null,
                tint = LocalContentColor.current.copy(alpha = 0.075f),
                modifier = Modifier.size(80.dp).align(Alignment.TopEnd).padding(top = 8.dp),
            )
            Column {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 4.dp),
                ) {
                    Text(
                        "Tests",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                Text(
                    "Your tests moved to a new tab. You can reach them through the navigation bar below.",
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp)
                        .padding(horizontal = 8.dp),
                ) {
                    TextButton(onClick = {}) {
                        Text(
                            "Dismiss",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {}) {
                        Text("See tests")
                    }
                }
            }
        }
    }
}


@Composable
private fun ColumnScope.BlogSection() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Text(
            "OONI News, Reports and Findings",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 20.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }

    OutlinedCard(
        onClick = {},
        modifier = Modifier.padding(top = 16.dp).padding(horizontal = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(IntrinsicSize.Min),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Information Controls in India and Pakistan During the May 2025 Conflict",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
                Text(
                    text = "3 days ago",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Image(
                painterResource(Res.drawable.donate_octopus_2),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(96.dp).background(Color(0xffbbdaf2)),
            )
        }
    }

    OutlinedCard(
        onClick = {},
        modifier = Modifier.padding(top = 16.dp).padding(horizontal = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(IntrinsicSize.Min),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Launch: New OONI Explorer thematic censorship pages",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
                Text(
                    text = "15 September, 2025",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Image(
                painterResource(Res.drawable.donate_octopus_1),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(96.dp).background(Color(0xfff23e3e)),
            )
        }
    }

    TextButton(
        onClick = {},
        modifier = Modifier.align(Alignment.End).padding(top = 8.dp, end = 16.dp),
    ) {
        Text("Read More")
    }
}

@Composable
private fun VpnWarning() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(Modifier.padding(8.dp)) {
            Icon(
                painterResource(Res.drawable.ic_warning),
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(stringResource(Res.string.Modal_DisableVPN_Title))
        }
    }
}

@Preview
@Composable
fun DashboardScreenPreview() {
    AppTheme {
        DashboardScreen(
            state = DashboardViewModel.State(),
            onEvent = {},
        )
    }
}

@Preview
@Composable
fun VpnWarningPreview() {
    AppTheme {
        VpnWarning()
    }
}
