package org.ooni.probe.ui.results

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.TaskOrigin_AutoRun
import ooniprobe.composeapp.generated.resources.TaskOrigin_Manual
import ooniprobe.composeapp.generated.resources.TestResults_NotAvailable
import ooniprobe.composeapp.generated.resources.TestResults_UnknownASN
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.ui.shared.relativeDateTime

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
            val asn = if (network?.isValid() == false) {
                stringResource(Res.string.TestResults_NotAvailable)
            } else {
                network?.asn ?: stringResource(Res.string.TestResults_UnknownASN)
            }
            val networkText = network?.networkName?.let { "$it " } + "($asn)"
            Text(
                networkText,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

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
