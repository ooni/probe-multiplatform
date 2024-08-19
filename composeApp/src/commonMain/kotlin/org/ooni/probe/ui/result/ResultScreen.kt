package org.ooni.probe.ui.result

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.back
import ooniprobe.composeapp.generated.resources.ic_settings
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.MeasurementWithUrl

@Composable
fun ResultScreen(
    state: ResultViewModel.State,
    onEvent: (ResultViewModel.Event) -> Unit,
) {
    Column {
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
                containerColor = state.result?.descriptor?.color
                    ?: MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )

        if (state.result == null) return@Column

        LazyColumn {
            items(state.result.measurements, key = { it.measurement.idOrThrow.value }) { item ->
                ResultMeasurementItem(item)
            }
        }
    }
}

@Composable
fun ResultMeasurementItem(item: MeasurementWithUrl) {
    val test = item.measurement.test
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(16.dp),
    ) {
        Icon(
            // TODO: Better fallback for nettest icon
            // TODO: Web categories icon
            painterResource(test.iconRes ?: Res.drawable.ic_settings),
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
