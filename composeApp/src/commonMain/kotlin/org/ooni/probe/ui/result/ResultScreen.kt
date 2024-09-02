package org.ooni.probe.ui.result

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.back
import ooniprobe.composeapp.generated.resources.ooni_empty_state
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementWithUrl

@Composable
fun ResultScreen(
    state: ResultViewModel.State,
    onEvent: (ResultViewModel.Event) -> Unit,
) {
    Column {
        val descriptorColor = state.result?.descriptor?.color ?: MaterialTheme.colorScheme.primary
        TopAppBar(
            title = {
                Text(state.result?.descriptor?.title?.invoke().orEmpty())
            },
            navigationIcon = {
                IconButton(onClick = { onEvent(ResultViewModel.Event.BackClicked) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                titleContentColor = descriptorColor,
                navigationIconContentColor = descriptorColor,
                actionIconContentColor = descriptorColor,
            ),
        )

        if (state.result == null) return@Column

        LazyColumn(
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        ) {
            items(state.result.measurements, key = { it.measurement.idOrThrow.value }) { item ->
                ResultMeasurementItem(
                    item = item,
                    onClick = { reportId, input ->
                        onEvent(ResultViewModel.Event.MeasurementClicked(reportId, input))
                    },
                )
            }
        }
    }
}

@Composable
private fun ResultMeasurementItem(
    item: MeasurementWithUrl,
    onClick: (MeasurementModel.ReportId, String?) -> Unit,
) {
    val test = item.measurement.test
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .run {
                if (item.measurement.isUploaded && item.measurement.reportId != null) {
                    clickable { onClick(item.measurement.reportId, item.url?.url) }
                } else {
                    alpha(0.5f)
                }
            }
            .padding(16.dp),
    ) {
        Icon(
            painterResource(
                if (test == TestType.WebConnectivity && item.url != null) {
                    item.url.category.icon
                } else {
                    test.iconRes ?: Res.drawable.ooni_empty_state
                },
            ),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp),
        )
        Text(
            text = if (test == TestType.WebConnectivity && item.url != null) {
                item.url.url
            } else if (test is TestType.Experimental) {
                test.name
            } else {
                stringResource(test.labelRes)
            },
            maxLines = 1,
        )
    }
}
