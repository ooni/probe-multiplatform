package org.ooni.probe.ui.result

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Snackbar_ResultsNotUploaded_Text
import ooniprobe.composeapp.generated.resources.ic_cloud_off
import ooniprobe.composeapp.generated.resources.ic_measurement_anomaly
import ooniprobe.composeapp.generated.resources.ic_measurement_failed
import ooniprobe.composeapp.generated.resources.ic_measurement_ok
import ooniprobe.composeapp.generated.resources.measurement_anomaly
import ooniprobe.composeapp.generated.resources.measurement_failed
import ooniprobe.composeapp.generated.resources.measurement_ok
import ooniprobe.composeapp.generated.resources.ooni_empty_state
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.MeasurementWithUrl

@Composable
fun ResultMeasurementCell(
    item: MeasurementWithUrl,
    isResultDone: Boolean,
    onClick: (MeasurementModel.ReportId, String?) -> Unit,
) {
    val measurement = item.measurement
    val test = measurement.test
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .run {
                if (measurement.isDone && !measurement.isMissingUpload) {
                    clickable { onClick(measurement.reportId!!, item.url?.url) }
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
            contentDescription = item.url?.category?.title?.let { stringResource(it) },
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
            modifier = Modifier.weight(1f),
        )
        if (isResultDone && measurement.isDoneAndMissingUpload) {
            Icon(
                painterResource(Res.drawable.ic_cloud_off),
                contentDescription = stringResource(Res.string.Snackbar_ResultsNotUploaded_Text),
                modifier = Modifier.padding(end = 16.dp).size(24.dp),
            )
        }
        if (measurement.isDone || isResultDone) {
            val isFailed = measurement.isFailed || (isResultDone && !measurement.isDone)
            Icon(
                painterResource(
                    when {
                        isFailed ->
                            Res.drawable.ic_measurement_failed
                        measurement.isAnomaly -> Res.drawable.ic_measurement_anomaly
                        else -> Res.drawable.ic_measurement_ok
                    },
                ),
                contentDescription = stringResource(
                    when {
                        isFailed -> Res.string.measurement_failed
                        measurement.isAnomaly -> Res.string.measurement_anomaly
                        else -> Res.string.measurement_ok
                    },
                ),
                tint = Color.Unspecified,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp),
            )
        } else if (!isResultDone) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp),
            )
        }
    }
}
