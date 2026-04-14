package org.ooni.probe.ui.results

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDateTime
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.TaskOrigin_AutoRun
import ooniprobe.composeapp.generated.resources.TaskOrigin_Manual
import ooniprobe.composeapp.generated.resources.TestResults_NotAvailable
import ooniprobe.composeapp.generated.resources.TestResults_UnknownASN
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.NetworkModel
import org.ooni.probe.data.models.RunModel
import org.ooni.probe.shared.now
import org.ooni.probe.ui.shared.relativeDateTime
import org.ooni.probe.ui.theme.AppTheme

@Composable
fun RunCell(item: RunListItem) {
    val network = item.run.network

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val networkName = if (network == null || !network.isValid()) {
                stringResource(Res.string.TestResults_UnknownASN)
            } else {
                network.name ?: stringResource(Res.string.TestResults_NotAvailable)
            }
            val networkAsn = if (network?.isValid() == true) network.asn else null
            Row(Modifier.wrapContentWidth()) {
                Text(
                    networkName,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                networkAsn?.let {
                    Text(
                        " ($it)",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                    )
                }
            }

            Text(
                item.run.startTime.relativeDateTime() + " – " + item.sourceText,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        if (!item.isDone) {
            CircularProgressIndicator(
                modifier = Modifier.padding(start = 8.dp).size(24.dp),
            )
        }
    }
}

private val RunListItem.sourceText
    @Composable
    get() = stringResource(
        when (run.taskOrigin) {
            TaskOrigin.AutoRun -> Res.string.TaskOrigin_AutoRun
            TaskOrigin.OoniRun -> Res.string.TaskOrigin_Manual
        },
    )

@Composable
@Preview
fun RunCellPreview() {
    AppTheme {
        RunCell(
            item = RunListItem(
                run = RunModel(
                    id = RunModel.Id("123"),
                    network = NetworkModel(
                        name = "Vodafone Itália",
                        asn = "AS12345",
                        countryCode = null,
                        networkType = null,
                    ),
                    startTime = LocalDateTime.now(),
                    taskOrigin = TaskOrigin.AutoRun,
                ),
                results = emptyList(),
            ),
        )
    }
}
