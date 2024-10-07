package org.ooni.probe.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.TestResults_NotAvailable
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Hero_DataUsage
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Hero_Tests
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Hero_Country
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Hero_DateAndTime
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Hero_Mobile
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Hero_Network
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Hero_NoInternet
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Hero_Runtime
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Hero_WiFi
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Performance_Hero_Download
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Performance_Hero_Upload
import ooniprobe.composeapp.generated.resources.back
import ooniprobe.composeapp.generated.resources.ic_download
import ooniprobe.composeapp.generated.resources.ic_upload
import ooniprobe.composeapp.generated.resources.vpn
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.NetworkType
import org.ooni.probe.data.models.ResultItem
import org.ooni.probe.ui.results.UploadResults
import org.ooni.probe.ui.shared.formatDataUsage
import org.ooni.probe.ui.shared.longFormat
import org.ooni.probe.ui.shared.shortFormat
import org.ooni.probe.ui.theme.LocalCustomColors

@Composable
fun ResultScreen(
    state: ResultViewModel.State,
    onEvent: (ResultViewModel.Event) -> Unit,
) {
    Column {
        val descriptorColor = state.result?.descriptor?.color ?: MaterialTheme.colorScheme.primary
        val onDescriptorColor = LocalCustomColors.current.onDescriptor
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
                containerColor = descriptorColor,
                scrolledContainerColor = descriptorColor,
                navigationIconContentColor = onDescriptorColor,
                titleContentColor = onDescriptorColor,
                actionIconContentColor = onDescriptorColor,
            ),
        )

        if (state.result == null) return@Column

        Surface(
            color = descriptorColor,
            contentColor = onDescriptorColor,
        ) {
            Summary(state.result)
        }

        if (state.result.anyMeasurementMissingUpload) {
            UploadResults(onUploadClick = { onEvent(ResultViewModel.Event.UploadClicked) })
        }

        LazyColumn(
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        ) {
            items(state.result.measurements, key = { it.measurement.idOrThrow.value }) { item ->
                ResultMeasurementCell(
                    item = item,
                    isResultDone = state.result.result.isDone,
                    onClick = { reportId, input ->
                        onEvent(ResultViewModel.Event.MeasurementClicked(reportId, input))
                    },
                )
            }
        }
    }
}

@Composable
private fun Summary(item: ResultItem) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    Box {
        HorizontalPager(
            state = pagerState,
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .padding(top = 8.dp, bottom = 16.dp)
                .defaultMinSize(minHeight = 128.dp),
        ) { page ->
            when (page) {
                0 -> SummaryDetails(item)
                1 -> SummaryNetwork(item)
            }
        }
        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(pagerState.pageCount) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .padding(bottom = 8.dp)
                        .alpha(if (pagerState.currentPage == index) 1f else 0.33f)
                        .clip(CircleShape)
                        .background(LocalContentColor.current)
                        .size(12.dp),
                )
            }
        }
    }
}

@Composable
private fun SummaryDetails(item: ResultItem) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        val labelModifier = Modifier.weight(4f)
        val valueModifier = Modifier.weight(6f)
        Row {
            Text(
                stringResource(Res.string.TestResults_Overview_Hero_Tests),
                fontWeight = FontWeight.Bold,
                modifier = labelModifier,
            )
            Text(
                item.measurements.size.toString(),
                modifier = valueModifier,
            )
        }
        Row {
            Text(
                stringResource(Res.string.TestResults_Summary_Hero_DateAndTime),
                fontWeight = FontWeight.Bold,
                modifier = labelModifier,
            )
            Text(
                item.result.startTime.longFormat(),
                modifier = valueModifier,
            )
        }
        Row {
            Text(
                stringResource(Res.string.TestResults_Overview_Hero_DataUsage),
                fontWeight = FontWeight.Bold,
                modifier = labelModifier,
            )
            Row(
                valueModifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(Res.drawable.ic_download),
                    contentDescription = stringResource(Res.string.TestResults_Summary_Performance_Hero_Download),
                    modifier = Modifier.padding(end = 4.dp).size(16.dp),
                )
                Text(item.result.dataUsageDown.formatDataUsage())
                Icon(
                    painterResource(Res.drawable.ic_upload),
                    contentDescription = stringResource(Res.string.TestResults_Summary_Performance_Hero_Upload),
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp).size(16.dp),
                )
                Text(item.result.dataUsageUp.formatDataUsage())
            }
        }
        Row {
            Text(
                stringResource(Res.string.TestResults_Summary_Hero_Runtime),
                fontWeight = FontWeight.Bold,
                modifier = labelModifier,
            )
            Text(
                item.totalRuntime.shortFormat(),
                modifier = valueModifier,
            )
        }
    }
}

@Composable
private fun SummaryNetwork(item: ResultItem) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        val labelModifier = Modifier.weight(3f)
        val valueModifier = Modifier.weight(7f)
        Row {
            Text(
                stringResource(Res.string.TestResults_Summary_Hero_Country),
                fontWeight = FontWeight.Bold,
                modifier = labelModifier,
            )
            Text(
                item.network?.countryCode
                    ?: stringResource(Res.string.TestResults_NotAvailable),
                modifier = valueModifier,
            )
        }
        Row {
            Text(
                stringResource(Res.string.TestResults_Summary_Hero_Network),
                fontWeight = FontWeight.Bold,
                modifier = labelModifier,
            )
            Text(
                item.network?.let { network ->
                    """
                    ${network.networkName.orEmpty()}
                    ${network.asn.orEmpty()} (${network.networkType?.label().orEmpty()})
                    """.trimIndent()
                } ?: stringResource(Res.string.TestResults_NotAvailable),
                modifier = valueModifier,
            )
        }
    }
}

@Composable
private fun NetworkType.label(): String =
    stringResource(
        when (this) {
            NetworkType.Mobile -> Res.string.TestResults_Summary_Hero_Mobile
            NetworkType.NoInternet -> Res.string.TestResults_Summary_Hero_NoInternet
            NetworkType.VPN -> Res.string.vpn
            NetworkType.Wifi -> Res.string.TestResults_Summary_Hero_WiFi
            is NetworkType.Unknown -> Res.string.TestResults_NotAvailable
        },
    )
