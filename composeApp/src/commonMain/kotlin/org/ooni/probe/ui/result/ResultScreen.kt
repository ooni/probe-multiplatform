package org.ooni.probe.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ooniprobe.composeapp.generated.resources.Common_Next
import ooniprobe.composeapp.generated.resources.Common_Previous
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Modal_ReRun_Title
import ooniprobe.composeapp.generated.resources.Modal_ReRun_Websites_Run
import ooniprobe.composeapp.generated.resources.Modal_ReRun_Websites_Title
import ooniprobe.composeapp.generated.resources.NetworkType_Vpn
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.TestResults_Kbps
import ooniprobe.composeapp.generated.resources.TestResults_NotAvailable
import ooniprobe.composeapp.generated.resources.TestResults_Overview_Hero_DataUsage
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Hero_Bluetooth
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Hero_Country
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Hero_DateAndTime
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Hero_Ethernet
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Hero_Mobile
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Hero_Network
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Hero_NoInternet
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Hero_Runtime
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Hero_Usb
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Hero_WiFi
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Performance_Hero_Download
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Performance_Hero_Ping
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Performance_Hero_Upload
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Performance_Hero_Video
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Performance_Hero_Video_Quality
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Websites_Hero_Blocked
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Websites_Hero_Reachable
import ooniprobe.composeapp.generated.resources.TestResults_Summary_Websites_Hero_Tested
import ooniprobe.composeapp.generated.resources.TestResults_ms
import ooniprobe.composeapp.generated.resources.ic_download
import ooniprobe.composeapp.generated.resources.ic_keyboard_arrow_left
import ooniprobe.composeapp.generated.resources.ic_keyboard_arrow_right
import ooniprobe.composeapp.generated.resources.ic_replay
import ooniprobe.composeapp.generated.resources.ic_upload
import ooniprobe.composeapp.generated.resources.ooni_bw
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.NetworkType
import org.ooni.engine.models.SummaryType
import org.ooni.probe.data.models.ResultItem
import org.ooni.probe.data.models.downloadSpeed
import org.ooni.probe.data.models.ping
import org.ooni.probe.data.models.uploadSpeed
import org.ooni.probe.data.models.videoQuality
import org.ooni.probe.shared.formatDataUsage
import org.ooni.probe.ui.result.ResultViewModel.MeasurementGroupItem.Group
import org.ooni.probe.ui.result.ResultViewModel.MeasurementGroupItem.Single
import org.ooni.probe.ui.results.UploadResults
import org.ooni.probe.ui.shared.NavigationBackButton
import org.ooni.probe.ui.shared.TopBar
import org.ooni.probe.ui.shared.VerticalScrollbar
import org.ooni.probe.ui.shared.format
import org.ooni.probe.ui.shared.isHeightCompact
import org.ooni.probe.ui.shared.longFormat
import org.ooni.probe.ui.theme.LocalCustomColors

@Composable
fun ResultScreen(
    state: ResultViewModel.State,
    onEvent: (ResultViewModel.Event) -> Unit,
) {
    var showRerunConfirmation by remember { mutableStateOf(false) }

    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        val descriptorColor = state.result?.descriptor?.color ?: MaterialTheme.colorScheme.primary
        val onDescriptorColor = LocalCustomColors.current.onDescriptor
        TopBar(
            title = {
                Text(
                    state.result
                        ?.descriptor
                        ?.title
                        ?.invoke()
                        .orEmpty(),
                )
            },
            navigationIcon = {
                NavigationBackButton({ onEvent(ResultViewModel.Event.BackClicked) })
            },
            actions = {
                if (state.result?.canBeRerun == true) {
                    IconButton(
                        onClick = { showRerunConfirmation = true },
                        enabled = state.rerunEnabled,
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_replay),
                            contentDescription = stringResource(Res.string.Modal_ReRun_Title),
                        )
                    }
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
        val showSummary = !isHeightCompact()

        Box(Modifier.fillMaxSize()) {
            val lazyListState = rememberLazyListState()
            LazyColumn(
                contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                state = lazyListState,
            ) {
                if (showSummary) {
                    item("summary") {
                        Surface(
                            color = descriptorColor,
                            contentColor = onDescriptorColor,
                        ) {
                            Summary(state.result)
                        }
                    }
                }

                if (state.result.anyMeasurementMissingUpload) {
                    stickyHeader("upload_results") {
                        UploadResults(onUploadClick = { onEvent(ResultViewModel.Event.UploadClicked) })
                    }
                }

                items(state.groupedMeasurements, key = { item ->
                    when (item) {
                        is Group -> item.test.name
                        is Single -> item.measurement.measurement.idOrThrow.value
                    }
                }) { item ->
                    when (item) {
                        is Group -> {
                            ResultGroupMeasurementCell(
                                item = item,
                                isResultDone = state.result.result.isDone,
                                onClick = { onEvent(ResultViewModel.Event.MeasurementClicked(it)) },
                                onDropdownToggled = {
                                    onEvent(ResultViewModel.Event.MeasurementGroupToggled(item))
                                },
                            )
                        }

                        is Single -> {
                            ResultMeasurementCell(
                                item = item.measurement,
                                isResultDone = state.result.result.isDone,
                                onClick = { onEvent(ResultViewModel.Event.MeasurementClicked(it)) },
                            )
                        }
                    }
                }
            }
            VerticalScrollbar(
                state = lazyListState,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }

    if (showRerunConfirmation) {
        RerunConfirmationDialog(
            websitesCount = state.result?.urlCount ?: 0,
            onDismiss = { showRerunConfirmation = false },
            onConfirm = { onEvent(ResultViewModel.Event.RerunClicked) },
        )
    }
}

@Composable
private fun Summary(item: ResultItem) {
    val summaryType = item.descriptor.summaryType
    val pages = summaryType.summaryPages
    val pagerState = rememberPagerState(pageCount = pages::size)
    val coroutineScope = rememberCoroutineScope()

    Box {
        Icon(
            painterResource(Res.drawable.ooni_bw),
            contentDescription = null,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 18.dp, y = 18.dp),
        )
        HorizontalPager(
            state = pagerState,
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .defaultMinSize(minHeight = 128.dp),
        ) { pageIndex ->
            pages[pageIndex](item)
        }
        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(pagerState.pageCount) { index ->
                Box(
                    modifier = Modifier
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }.padding(16.dp)
                        .alpha(if (pagerState.currentPage == index) 1f else 0.33f)
                        .clip(CircleShape)
                        .background(LocalContentColor.current)
                        .size(12.dp),
                )
            }
        }
        IconButton(
            onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(
                        (pagerState.currentPage - 1).coerceAtLeast(0),
                    )
                }
            },
            enabled = pagerState.currentPage != 0,
            modifier = Modifier.fillMaxHeight().align(Alignment.BottomStart),
        ) {
            Icon(
                painterResource(Res.drawable.ic_keyboard_arrow_left),
                contentDescription = stringResource(Res.string.Common_Previous),
            )
        }
        IconButton(
            onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(
                        (pagerState.currentPage + 1).coerceAtMost(pagerState.pageCount - 1),
                    )
                }
            },
            enabled = pagerState.currentPage != pagerState.pageCount - 1,
            modifier = Modifier.fillMaxHeight().align(Alignment.BottomEnd),
        ) {
            Icon(
                painterResource(Res.drawable.ic_keyboard_arrow_right),
                contentDescription = stringResource(Res.string.Common_Next),
            )
        }
    }
}

private val SummaryType.summaryPages: List<@Composable (ResultItem) -> Unit>
    get() = when (this) {
        SummaryType.Simple -> listOf(
            { SummaryDetails(it) },
            { SummaryNetwork(it) },
        )

        SummaryType.Anomaly -> listOf(
            { SummaryStats(it) },
            { SummaryDetails(it) },
            { SummaryNetwork(it) },
        )

        SummaryType.Performance -> listOf(
            { SummaryPerformance(it) },
            { SummaryDetails(it) },
            { SummaryNetwork(it) },
        )
    }

@Composable
private fun SummaryStats(item: ResultItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min) // So VerticalDividers don't expand to the whole screen
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                item.measurementCounts.total.toString(),
                style = MaterialTheme.typography.headlineMedium,
            )

            Text(
                pluralStringResource(
                    Res.plurals.TestResults_Summary_Websites_Hero_Tested,
                    item.measurementCounts.total.toInt(),
                ),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        VerticalDivider(Modifier.padding(4.dp), color = LocalContentColor.current)

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                item.measurementCounts.anomaly.toString(),
                style = MaterialTheme.typography.headlineMedium,
            )

            Text(
                pluralStringResource(
                    Res.plurals.TestResults_Summary_Websites_Hero_Blocked,
                    item.measurementCounts.anomaly.toInt(),
                ),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        VerticalDivider(Modifier.padding(4.dp), color = LocalContentColor.current)

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                item.measurementCounts.succeeded.toString(),
                style = MaterialTheme.typography.headlineMedium,
            )

            Text(
                pluralStringResource(
                    Res.plurals.TestResults_Summary_Websites_Hero_Reachable,
                    item.measurementCounts.succeeded.toInt(),
                ),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
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
                item.totalRuntime.format(),
                modifier = valueModifier,
            )
        }
    }
}

@Composable
private fun SummaryPerformance(item: ResultItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(Res.string.TestResults_Summary_Performance_Hero_Video),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp),
                maxLines = 1,
            )

            Text(
                stringResource(
                    item.testKeys?.videoQuality() ?: Res.string.TestResults_NotAvailable,
                ),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
            )

            Text(
                stringResource(Res.string.TestResults_Summary_Performance_Hero_Video_Quality),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp),
                maxLines = 1,
            )
        }

        VerticalDivider(Modifier.padding(4.dp), color = LocalContentColor.current)

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(Res.string.TestResults_Summary_Performance_Hero_Download),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp),
                maxLines = 1,
            )

            item.testKeys?.downloadSpeed()?.let { (downloadSpeed, unit) ->

                Text(
                    downloadSpeed,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                )

                Text(
                    stringResource(unit),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                    maxLines = 1,
                )
            } ?: run {
                Text(
                    stringResource(Res.string.TestResults_NotAvailable),
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                )

                Text(
                    stringResource(Res.string.TestResults_Kbps),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                    maxLines = 1,
                )
            }
        }

        VerticalDivider(Modifier.padding(4.dp), color = LocalContentColor.current)

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(Res.string.TestResults_Summary_Performance_Hero_Upload),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp),
                maxLines = 1,
            )
            item.testKeys?.uploadSpeed()?.let { (uploadSpeed, unit) ->

                Text(
                    uploadSpeed,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                )

                Text(
                    stringResource(unit),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                    maxLines = 1,
                )
            } ?: run {
                Text(
                    stringResource(Res.string.TestResults_NotAvailable),
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                )

                Text(
                    stringResource(Res.string.TestResults_Kbps),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                    maxLines = 1,
                )
            }
        }

        VerticalDivider(Modifier.padding(4.dp), color = LocalContentColor.current)

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(Res.string.TestResults_Summary_Performance_Hero_Ping),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp),
                maxLines = 1,
            )

            Text(
                item.testKeys?.ping()
                    ?: stringResource(Res.string.TestResults_NotAvailable),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
            )

            Text(
                stringResource(Res.string.TestResults_ms),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp),
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
                item.network?.countryCode ?: stringResource(Res.string.TestResults_NotAvailable),
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
                    val asn = when {
                        !item.network.isValid() -> stringResource(Res.string.TestResults_NotAvailable)
                        else -> network.asn.orEmpty()
                    }
                    """
                    ${network.networkName.orEmpty()}
                    $asn (${network.networkType?.label().orEmpty()})
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
            NetworkType.VPN -> Res.string.NetworkType_Vpn
            NetworkType.Wifi -> Res.string.TestResults_Summary_Hero_WiFi
            is NetworkType.Unknown -> Res.string.TestResults_NotAvailable
            NetworkType.Bluetooth -> Res.string.TestResults_Summary_Hero_Bluetooth
            NetworkType.Ethernet -> Res.string.TestResults_Summary_Hero_Ethernet
            NetworkType.Usb -> Res.string.TestResults_Summary_Hero_Usb
        },
    )

@Composable
private fun RerunConfirmationDialog(
    websitesCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.Modal_ReRun_Title)) },
        text = { Text(stringResource(Res.string.Modal_ReRun_Websites_Title, websitesCount)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.Modal_ReRun_Websites_Run))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.Modal_Cancel))
            }
        },
    )
}
