package org.ooni.probe.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import ooniprobe.composeapp.generated.resources.Common_Dismiss
import ooniprobe.composeapp.generated.resources.Common_Month
import ooniprobe.composeapp.generated.resources.Common_Today
import ooniprobe.composeapp.generated.resources.Common_Total
import ooniprobe.composeapp.generated.resources.Common_Week
import ooniprobe.composeapp.generated.resources.Dashboard_Articles_ReadMore
import ooniprobe.composeapp.generated.resources.Dashboard_Articles_Title
import ooniprobe.composeapp.generated.resources.Dashboard_AutoRun_Disabled
import ooniprobe.composeapp.generated.resources.Dashboard_AutoRun_Enabled
import ooniprobe.composeapp.generated.resources.Dashboard_LastResults
import ooniprobe.composeapp.generated.resources.Dashboard_LastResults_SeeResults
import ooniprobe.composeapp.generated.resources.Dashboard_Stats_Countries
import ooniprobe.composeapp.generated.resources.Dashboard_Stats_Empty
import ooniprobe.composeapp.generated.resources.Dashboard_Stats_Networks
import ooniprobe.composeapp.generated.resources.Dashboard_Stats_Title
import ooniprobe.composeapp.generated.resources.Dashboard_TestsMoved_Action
import ooniprobe.composeapp.generated.resources.Dashboard_TestsMoved_Description
import ooniprobe.composeapp.generated.resources.Dashboard_TestsMoved_Title
import ooniprobe.composeapp.generated.resources.Measurements_Failed
import ooniprobe.composeapp.generated.resources.Modal_DisableVPN_Title
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Websites_Blocked
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Websites_Tested
import ooniprobe.composeapp.generated.resources.app_name
import ooniprobe.composeapp.generated.resources.dashboard_arc
import ooniprobe.composeapp.generated.resources.ic_auto_run
import ooniprobe.composeapp.generated.resources.ic_heart
import ooniprobe.composeapp.generated.resources.ic_history
import ooniprobe.composeapp.generated.resources.ic_measurement_anomaly
import ooniprobe.composeapp.generated.resources.ic_measurement_failed
import ooniprobe.composeapp.generated.resources.ic_tests
import ooniprobe.composeapp.generated.resources.ic_warning
import ooniprobe.composeapp.generated.resources.ic_world
import ooniprobe.composeapp.generated.resources.logo_probe
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.ArticleModel
import org.ooni.probe.data.models.MeasurementStats
import org.ooni.probe.data.models.Run
import org.ooni.probe.data.models.RunBackgroundState
import org.ooni.probe.shared.largeNumberShort
import org.ooni.probe.ui.articles.ArticleCard
import org.ooni.probe.ui.shared.IgnoreBatteryOptimizationDialog
import org.ooni.probe.ui.shared.TestRunErrorMessages
import org.ooni.probe.ui.shared.VerticalScrollbar
import org.ooni.probe.ui.shared.isHeightCompact
import org.ooni.probe.ui.shared.relativeDateTime
import org.ooni.probe.ui.theme.AppTheme
import org.ooni.probe.ui.theme.LocalCustomColors
import org.ooni.probe.ui.theme.cardTitle
import org.ooni.probe.ui.theme.dashboardSectionTitle

@Composable
fun DashboardScreen(
    state: DashboardViewModel.State,
    onEvent: (DashboardViewModel.Event) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
    ) {
        // Colorful top background with logo
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

        // Run Section
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

                if (state.runBackgroundState is RunBackgroundState.Idle) {
                    AutoRunButton(isAutoRunEnabled = state.isAutoRunEnabled, onEvent)
                }
            }
        }

        // Scrollable Content
        Box(Modifier.fillMaxSize()) {
            val scrollState = rememberScrollState()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(scrollState)
                    .padding(bottom = 16.dp)
                    .fillMaxSize(),
            ) {
                if (state.showVpnWarning) {
                    VpnWarning()
                }

                if (state.runBackgroundState is RunBackgroundState.Idle && state.lastRun != null) {
                    LastRun(state.lastRun, onEvent)
                }

                if (state.showTestsMovedNotice) {
                    TestsMoved(onEvent)
                }

                StatsSection(state.stats)
                ArticlesSection(state.articles, state.showReadMoreArticles, onEvent)
            }
            VerticalScrollbar(state = scrollState, modifier = Modifier.align(Alignment.CenterEnd))
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

    LifecycleResumeEffect(Unit) {
        onEvent(DashboardViewModel.Event.Resumed)
        onPauseOrDispose { onEvent(DashboardViewModel.Event.Paused) }
    }
}

@Composable
private fun AutoRunButton(
    isAutoRunEnabled: Boolean,
    onEvent: (DashboardViewModel.Event) -> Unit,
) {
    TextButton(
        onClick = { onEvent(DashboardViewModel.Event.AutoRunClicked) },
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(Res.drawable.ic_auto_run),
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp).size(16.dp),
            )
            Text(
                text = autoRunText(isAutoRunEnabled),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun autoRunText(isAutoRunEnabled: Boolean): AnnotatedString {
    val baseString = stringResource(
        if (isAutoRunEnabled) {
            Res.string.Dashboard_AutoRun_Enabled
        } else {
            Res.string.Dashboard_AutoRun_Disabled
        },
    )
    val startSection = baseString.indexOf("<i>")
    val endSection = baseString.indexOf("</i>")
    val sectionColor = if (isAutoRunEnabled) {
        LocalCustomColors.current.success
    } else {
        MaterialTheme.colorScheme.error
    }
    return if (startSection != -1 && endSection != -1 && startSection + 3 < endSection) {
        buildAnnotatedString {
            append(baseString.substring(0, startSection))
            withStyle(style = SpanStyle(color = sectionColor)) {
                append(baseString.substring(startSection + 3, endSection))
            }
            append(baseString.substring(endSection + 4, baseString.length))
        }
    } else {
        buildAnnotatedString { append(baseString) }
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

@Composable
private fun LastRun(
    run: Run,
    onEvent: (DashboardViewModel.Event) -> Unit,
) {
    DashboardCard(
        title = {
            Text(
                stringResource(Res.string.Dashboard_LastResults),
                style = MaterialTheme.typography.cardTitle,
            )
            Text(
                run.startTime.relativeDateTime(),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
            )
        },
        content = {
            FlowRow {
                ResultChip(
                    text = pluralStringResource(
                        Res.plurals.TestResults_Overview_Websites_Tested,
                        run.measurementCounts.tested.toInt(),
                        run.measurementCounts.tested,
                    ),
                    icon = Res.drawable.ic_world,
                    modifier = Modifier.padding(end = 2.dp),
                )

                ResultChip(
                    text = pluralStringResource(
                        Res.plurals.TestResults_Overview_Websites_Blocked,
                        run.measurementCounts.anomaly.toInt(),
                        run.measurementCounts.anomaly,
                    ),
                    icon = Res.drawable.ic_measurement_anomaly,
                    iconTint = LocalCustomColors.current.logWarn,
                    modifier = Modifier.padding(end = 2.dp),
                )

                ResultChip(
                    text = pluralStringResource(
                        Res.plurals.Measurements_Failed,
                        run.measurementCounts.failed.toInt(),
                        run.measurementCounts.failed,
                    ),
                    icon = Res.drawable.ic_measurement_failed,
                    iconTint = MaterialTheme.colorScheme.error,
                    modifier = Modifier,
                )
            }
        },
        startActions = {
            TextButton(onClick = { onEvent(DashboardViewModel.Event.DismissResultsClicked) }) {
                Text(
                    stringResource(Res.string.Common_Dismiss),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                )
            }
        },
        endActions = {
            TextButton(onClick = { onEvent(DashboardViewModel.Event.SeeResultsClicked) }) {
                Text(stringResource(Res.string.Dashboard_LastResults_SeeResults))
            }
        },
        icon = painterResource(Res.drawable.ic_history),
    )
}

@Composable
fun ResultChip(
    text: String,
    icon: DrawableResource,
    modifier: Modifier = Modifier,
    iconTint: Color? = null,
) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(text = text) },
        leadingIcon = {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = iconTint ?: LocalContentColor.current,
                modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            disabledLabelColor = LocalContentColor.current,
            disabledLeadingIconContentColor = LocalContentColor.current,
        ),
        border = AssistChipDefaults.assistChipBorder(true),
        modifier = modifier,
    )
}

@Composable
private fun TestsMoved(onEvent: (DashboardViewModel.Event) -> Unit) {
    DashboardCard(
        title = {
            Text(
                stringResource(Res.string.Dashboard_TestsMoved_Title),
                style = MaterialTheme.typography.cardTitle,
            )
        },
        content = {
            Text(stringResource(Res.string.Dashboard_TestsMoved_Description))
        },
        startActions = {
            TextButton(onClick = { onEvent(DashboardViewModel.Event.DismissTestsMovedClicked) }) {
                Text(
                    stringResource(Res.string.Common_Dismiss),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                )
            }
        },
        endActions = {
            TextButton(onClick = { onEvent(DashboardViewModel.Event.SeeTestsClicked) }) {
                Text(stringResource(Res.string.Dashboard_TestsMoved_Action))
            }
        },
        icon = painterResource(Res.drawable.ic_tests),
    )
}

@Composable
private fun StatsSection(stats: MeasurementStats?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            stringResource(Res.string.Dashboard_Stats_Title),
            style = MaterialTheme.typography.dashboardSectionTitle,
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
                value?.largeNumberShort().orEmpty(),
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
        if (stats?.measurementsTotal == 0L) {
            Text(stringResource(Res.string.Dashboard_Stats_Empty))
        } else {
            StatsEntry(stringResource(Res.string.Common_Today), stats?.measurementsToday)
            StatsEntry(stringResource(Res.string.Common_Week), stats?.measurementsWeek)
            StatsEntry(stringResource(Res.string.Common_Month), stats?.measurementsMonth)
            StatsEntry(stringResource(Res.string.Common_Total), stats?.measurementsTotal)
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
private fun ArticlesSection(
    articles: List<ArticleModel>,
    showReadMore: Boolean,
    onEvent: (DashboardViewModel.Event) -> Unit,
) {
    if (!OrganizationConfig.hasOoniNews || articles.isEmpty()) return

    HorizontalDivider(
        thickness = Dp.Hairline,
        modifier = Modifier.padding(vertical = 16.dp),
    )

    Text(
        stringResource(Res.string.Dashboard_Articles_Title),
        style = MaterialTheme.typography.dashboardSectionTitle,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    )

    articles.forEach { article ->
        ArticleCard(
            article = article,
            onClick = { onEvent(DashboardViewModel.Event.ArticleClicked(article)) },
        )
    }

    if (showReadMore) {
        TextButton(
            onClick = { onEvent(DashboardViewModel.Event.ReadMoreArticlesClicked) },
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(Res.string.Dashboard_Articles_ReadMore),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
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
