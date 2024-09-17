package org.ooni.probe.ui.descriptor.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Dashboard_ReviewDescriptor_Button_Default
import ooniprobe.composeapp.generated.resources.Dashboard_ReviewDescriptor_Button_Last
import ooniprobe.composeapp.generated.resources.Dashboard_ReviewDescriptor_Label
import ooniprobe.composeapp.generated.resources.DescriptorUpdate_Updates
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.ui.dashboard.TestDescriptorLabel

@Composable
fun ReviewUpdatesScreen(
    state: ReviewUpdatesViewModel.State,
    onEvent: (ReviewUpdatesViewModel.Event) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = state.currentDescriptorIndex, pageCount = {
        state.descriptors.size
    })
    val currentDescriptorIndex = state.currentDescriptorIndex + 1
    Column {
        TopAppBar(
            title = { Text(stringResource(Res.string.Dashboard_ReviewDescriptor_Label, currentDescriptorIndex, state.descriptors.size)) },
            actions = {
                IconButton(
                    onClick = {
                        onEvent(
                            ReviewUpdatesViewModel.Event.CancelClicked,
                        )
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(Res.string.Modal_Cancel),
                    )
                }
            },
        )
        Box(modifier = Modifier.fillMaxHeight().padding(16.dp)) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
            ) { page ->
                ReviewItem(state.currentDescriptor)
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(top = 32.dp, bottom = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    onClick = {
                        onEvent(ReviewUpdatesViewModel.Event.CancelClicked)
                    },
                ) {
                    Text(
                        stringResource(Res.string.Modal_Cancel),
                    )
                }

                TextButton(
                    onClick = {
                        onEvent(ReviewUpdatesViewModel.Event.UpdateDescriptorClicked(state.currentDescriptorIndex))
                    },
                ) {
                    Text(
                        stringResource(
                            when (currentDescriptorIndex == state.descriptors.size) {
                                true -> Res.string.Dashboard_ReviewDescriptor_Button_Last
                                false -> Res.string.Dashboard_ReviewDescriptor_Button_Default
                            },
                            currentDescriptorIndex,
                            state.descriptors.size,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewItem(currentDescriptor: Descriptor) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TestDescriptorLabel(currentDescriptor)

        currentDescriptor.shortDescription()?.let { shortDescription ->
            Text(
                shortDescription,
            )
        }
        Text(
            text = stringResource(Res.string.DescriptorUpdate_Updates),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp),
        )
        currentDescriptor.netTests.forEach { test ->
            Text(
                text = if (test.test is TestType.Experimental) {
                    test.test.name
                } else {
                    stringResource(test.test.labelRes)
                },
            )
            test.inputs?.forEach { input ->
                Text(
                    text = input,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
    }
}
