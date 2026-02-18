package org.ooni.probe.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ooniprobe.composeapp.generated.resources.Common_Month
import ooniprobe.composeapp.generated.resources.Common_Today
import ooniprobe.composeapp.generated.resources.Common_Total
import ooniprobe.composeapp.generated.resources.Common_Week
import ooniprobe.composeapp.generated.resources.Dashboard_Stats_Countries
import ooniprobe.composeapp.generated.resources.Dashboard_Stats_Empty
import ooniprobe.composeapp.generated.resources.Dashboard_Stats_Networks
import ooniprobe.composeapp.generated.resources.Dashboard_Stats_Title
import ooniprobe.composeapp.generated.resources.Modal_OK
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.ic_heart
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.MeasurementStats
import org.ooni.probe.shared.largeNumberShort
import org.ooni.probe.ui.theme.AppTheme
import org.ooni.probe.ui.theme.dashboardSectionTitle

@Composable
fun ColumnScope.StatsSection(stats: MeasurementStats?) {
    var showCountriesDialog by remember { mutableStateOf(false) }
    val countriesCount = stats?.countries?.size ?: 0

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
        value: Number?,
        modifier: Modifier = Modifier,
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
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
                    countriesCount,
                ),
                countriesCount,
                modifier = Modifier.run {
                    if (countriesCount > 0) {
                        clickable { showCountriesDialog = true }
                    } else {
                        this
                    }
                },
            )
        }
    }

    if (showCountriesDialog) {
        AlertDialog(
            onDismissRequest = { showCountriesDialog = false },
            title = {
                Text(
                    pluralStringResource(
                        Res.plurals.Dashboard_Stats_Countries,
                        stats?.countries?.size ?: 0,
                    ),
                )
            },
            text = {
                Text(
                    stats?.countries.orEmpty().joinToString(", "),
                )
            },
            confirmButton = {
                TextButton(onClick = { showCountriesDialog = false }) {
                    Text(stringResource(Res.string.Modal_OK))
                }
            },
        )
    }
}

@Preview
@Composable
fun StatsSectionPreview() {
    AppTheme {
        Column {
            StatsSection(
                stats = MeasurementStats(
                    measurementsToday = 6,
                    measurementsWeek = 12,
                    measurementsMonth = 1000,
                    measurementsTotal = 1234567,
                    networks = 3,
                    countries = listOf("Portugal", "Italy"),
                ),
            )
        }
    }
}
