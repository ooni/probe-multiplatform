package org.ooni.probe.ui.descriptor.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Dashboard_ReviewDescriptor_Button_Default
import ooniprobe.composeapp.generated.resources.Dashboard_ReviewDescriptor_Button_Last
import ooniprobe.composeapp.generated.resources.Dashboard_ReviewDescriptor_Label
import ooniprobe.composeapp.generated.resources.Dashboard_ReviewDescriptor_Reject
import ooniprobe.composeapp.generated.resources.DescriptorUpdate_Updates
import ooniprobe.composeapp.generated.resources.Modal_Cancel
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.ic_close
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.DescriptorItem
import org.ooni.probe.ui.dashboard.TestDescriptorLabel
import org.ooni.probe.ui.shared.TopBar

@Composable
fun ReviewUpdatesScreen(
    state: ReviewUpdatesViewModel.State,
    onEvent: (ReviewUpdatesViewModel.Event) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = state.index, pageCount = {
        state.descriptors.size
    })
    val currentDescriptorIndex = state.index + 1
    Column(Modifier.background(MaterialTheme.colorScheme.background)) {
        TopBar(
            title = {
                Text(
                    stringResource(
                        Res.string.Dashboard_ReviewDescriptor_Label,
                        currentDescriptorIndex,
                        state.descriptors.size,
                    ),
                )
            },
            actions = {
                IconButton(
                    onClick = {
                        onEvent(
                            ReviewUpdatesViewModel.Event.BackClicked,
                        )
                    },
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_close),
                        contentDescription = stringResource(Res.string.Modal_Cancel),
                    )
                }
            },
        )
        Box(modifier = Modifier.fillMaxHeight().padding(16.dp)) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
            ) {
                state.currentDescriptor?.let {
                    ReviewItem(it)
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(vertical = 32.dp)
                    .fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        onClick = { onEvent(ReviewUpdatesViewModel.Event.RejectClicked) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text(stringResource(Res.string.Dashboard_ReviewDescriptor_Reject))
                    }

                    Button(
                        onClick = { onEvent(ReviewUpdatesViewModel.Event.UpdateClicked) },
                    ) {
                        Text(
                            stringResource(
                                if (currentDescriptorIndex == state.descriptors.size) {
                                    Res.string.Dashboard_ReviewDescriptor_Button_Last
                                } else {
                                    Res.string.Dashboard_ReviewDescriptor_Button_Default
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
}

@Composable
fun ReviewItem(currentDescriptor: DescriptorItem) {
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
