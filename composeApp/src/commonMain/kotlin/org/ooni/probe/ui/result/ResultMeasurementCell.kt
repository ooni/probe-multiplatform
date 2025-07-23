package org.ooni.probe.ui.result

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Common_Collapse
import ooniprobe.composeapp.generated.resources.Common_Expand
import ooniprobe.composeapp.generated.resources.Measurements_Anomaly
import ooniprobe.composeapp.generated.resources.Measurements_Failed
import ooniprobe.composeapp.generated.resources.Measurements_Ok
import ooniprobe.composeapp.generated.resources.Modal_UploadFailed_Title
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Snackbar_ResultsNotUploaded_Text
import ooniprobe.composeapp.generated.resources.ic_cloud_off
import ooniprobe.composeapp.generated.resources.ic_keyboard_arrow_down
import ooniprobe.composeapp.generated.resources.ic_keyboard_arrow_up
import ooniprobe.composeapp.generated.resources.ic_measurement_anomaly
import ooniprobe.composeapp.generated.resources.ic_measurement_failed
import ooniprobe.composeapp.generated.resources.ic_measurement_ok
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.MeasurementWithUrl
import org.ooni.probe.ui.result.ResultViewModel.MeasurementGroupItem.Group

@Composable
fun ResultMeasurementCell(
    item: MeasurementWithUrl,
    isResultDone: Boolean,
    onClick: (MeasurementWithUrl) -> Unit,
) {
    val measurement = item.measurement
    val test = measurement.test
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .let {
                if (measurement.isDone && !measurement.isFailed) {
                    it.clickable { onClick(item) }
                } else {
                    it
                }
            }.alpha(if (measurement.isDone && !measurement.isMissingUpload) 1f else 0.66f)
            .padding(16.dp),
    ) {
        TestName(test, item, modifier = Modifier.weight(1f))
        if (isResultDone && measurement.isDoneAndMissingUpload) {
            Icon(
                painterResource(Res.drawable.ic_cloud_off),
                contentDescription = stringResource(
                    if (measurement.isUploadFailed) {
                        Res.string.Modal_UploadFailed_Title
                    } else {
                        Res.string.Snackbar_ResultsNotUploaded_Text
                    },
                ),
                tint = if (measurement.isUploadFailed) {
                    MaterialTheme.colorScheme.error
                } else {
                    LocalContentColor.current
                },
                modifier = Modifier.padding(end = 16.dp).size(24.dp),
            )
        }
        if (measurement.isDone || isResultDone) {
            val isFailed = measurement.isFailed || (isResultDone && !measurement.isDone)
            Icon(
                painterResource(
                    when {
                        isFailed -> Res.drawable.ic_measurement_failed
                        measurement.isAnomaly -> Res.drawable.ic_measurement_anomaly
                        else -> Res.drawable.ic_measurement_ok
                    },
                ),
                contentDescription = stringResource(
                    when {
                        isFailed -> Res.string.Measurements_Failed
                        measurement.isAnomaly -> Res.string.Measurements_Anomaly
                        else -> Res.string.Measurements_Ok
                    },
                ),
                tint = Color.Unspecified,
                modifier = Modifier.padding(end = 16.dp).size(24.dp),
            )
        } else if (!isResultDone) {
            CircularProgressIndicator(
                modifier = Modifier.padding(end = 16.dp).size(24.dp),
            )
        }
    }
}

@Composable
private fun TestName(
    test: TestType,
    item: MeasurementWithUrl,
    modifier: Modifier = Modifier,
) {
    val iconResource = when {
        test == TestType.WebConnectivity && item.url != null -> item.url.category.icon
        else -> test.iconRes
    }

    val contentDescription = item.url
        ?.category
        ?.title
        ?.let { stringResource(it) } ?: ""
    iconResource?.let { resource ->
        Icon(
            painterResource(resource),
            contentDescription = contentDescription,
            modifier = Modifier.padding(end = 16.dp).size(24.dp),
        )
    }
    Text(
        text = if (test == TestType.WebConnectivity && item.url != null) {
            item.url.url
        } else if (test is TestType.Experimental) {
            test.name
        } else {
            stringResource(test.labelRes)
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(end = 8.dp),
    )
}

@Composable
fun ResultGroupMeasurementCell(
    item: Group,
    isResultDone: Boolean,
    onClick: (MeasurementWithUrl) -> Unit,
    onDropdownToggled: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDropdownToggled() }
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        TestName(item.test, item.measurements.first(), modifier = Modifier.weight(1f))
        IconButton(onClick = { onDropdownToggled() }) {
            Icon(
                painterResource(
                    if (item.isExpanded) {
                        Res.drawable.ic_keyboard_arrow_up
                    } else {
                        Res.drawable.ic_keyboard_arrow_down
                    },
                ),
                contentDescription = stringResource(
                    if (item.isExpanded) {
                        Res.string.Common_Collapse
                    } else {
                        Res.string.Common_Expand
                    },
                ),
            )
        }
    }

    if (!item.isExpanded) return

    Column(modifier = Modifier.padding(start = 32.dp)) {
        item.measurements.forEach { measurement ->
            ResultMeasurementCell(measurement, isResultDone, onClick)
        }
    }
}
