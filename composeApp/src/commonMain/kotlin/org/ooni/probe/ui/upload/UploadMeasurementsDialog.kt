package org.ooni.probe.ui.upload

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Modal_OK
import ooniprobe.composeapp.generated.resources.Modal_ResultsNotUploaded_Uploading
import ooniprobe.composeapp.generated.resources.Modal_Retry
import ooniprobe.composeapp.generated.resources.Modal_UploadFailed_Paragraph
import ooniprobe.composeapp.generated.resources.Modal_UploadFailed_Title
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.Toast_ResultsUploaded
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.LocalSnackbarHostState
import org.ooni.probe.domain.UploadMissingMeasurements

@Composable
fun UploadMeasurementsDialog(
    state: UploadMissingMeasurements.State,
    onEvent: (UploadMeasurementsViewModel.Event) -> Unit,
) {
    Surface {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            when (state) {
                UploadMissingMeasurements.State.Starting -> {}
                is UploadMissingMeasurements.State.Uploading -> {
                    val progress = state.uploaded + state.failedToUpload
                    Text(
                        stringResource(
                            Res.string.Modal_ResultsNotUploaded_Uploading,
                            "$progress/${state.total}",
                        ),
                    )
                    LinearProgressIndicator(
                        progress = { progress / state.total.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                            .padding(top = 16.dp)
                            .height(8.dp),
                    )
                    TextButton(
                        onClick = { onEvent(UploadMeasurementsViewModel.Event.CancelClick) },
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        Text(stringResource(Res.string.Modal_Cancel))
                    }
                }

                is UploadMissingMeasurements.State.Finished -> {
                    if (state.failedToUpload == 0) {
                        val snackbarHostState = LocalSnackbarHostState.current
                        LaunchedEffect(Unit) {
                            snackbarHostState
                                ?.showSnackbar(getString(Res.string.Toast_ResultsUploaded))
                        }
                    } else {
                        Text(
                            stringResource(Res.string.Modal_UploadFailed_Title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                        Text(
                            stringResource(
                                Res.string.Modal_UploadFailed_Paragraph,
                                state.failedToUpload,
                                state.total,
                            ),
                        )
                        Row(modifier = Modifier.padding(top = 16.dp)) {
                            TextButton(onClick = { onEvent(UploadMeasurementsViewModel.Event.CloseClick) }) {
                                Text(stringResource(Res.string.Modal_OK))
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { onEvent(UploadMeasurementsViewModel.Event.RetryClick) }) {
                                Text(stringResource(Res.string.Modal_Retry))
                            }
                        }
                    }
                }
            }
        }
    }
}
