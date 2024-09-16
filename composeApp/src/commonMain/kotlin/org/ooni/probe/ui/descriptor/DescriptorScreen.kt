package org.ooni.probe.ui.descriptor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.AddDescriptor_AutoRun
import ooniprobe.composeapp.generated.resources.AddDescriptor_Settings
import ooniprobe.composeapp.generated.resources.Dashboard_Overview_Estimated
import ooniprobe.composeapp.generated.resources.Dashboard_Overview_LastRun_Never
import ooniprobe.composeapp.generated.resources.Dashboard_Overview_LatestTest
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.back
import ooniprobe.composeapp.generated.resources.ooni_empty_state
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.TestType
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.config.TestDisplayMode
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.ui.shared.MarkdownViewer
import org.ooni.probe.ui.shared.SelectableItem
import org.ooni.probe.ui.shared.relativeDateTime
import org.ooni.probe.ui.shared.shortFormat

@Composable
fun DescriptorScreen(
    state: DescriptorViewModel.State,
    onEvent: (DescriptorViewModel.Event) -> Unit,
) {
    val descriptor = state.descriptor ?: return

    Column {
        val descriptorColor = descriptor.color ?: MaterialTheme.colorScheme.primary
        TopAppBar(
            title = {
                Text(descriptor.title.invoke())
            },
            navigationIcon = {
                IconButton(onClick = { onEvent(DescriptorViewModel.Event.BackClicked) }) {
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .padding(bottom = 32.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
                    .padding(8.dp),
            ) {
                descriptor.icon?.let { icon ->
                    Icon(
                        painterResource(icon),
                        contentDescription = null,
                        tint = descriptorColor,
                        modifier = Modifier.size(64.dp),
                    )
                }

                Row {
                    Text(stringResource(Res.string.Dashboard_Overview_Estimated))

                    descriptor.dataUsage()?.let { dataUsage ->
                        Text(
                            text = dataUsage,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }

                    state.estimatedTime?.let { time ->
                        Text(
                            text = "~ ${time.shortFormat()}",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }

                Row {
                    Text(stringResource(Res.string.Dashboard_Overview_LatestTest))

                    Text(
                        text = state.lastResult?.startTime?.relativeDateTime()
                            ?: stringResource(Res.string.Dashboard_Overview_LastRun_Never),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            MarkdownViewer(
                markdown = descriptor.description().orEmpty(),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )

            Text(
                stringResource(Res.string.AddDescriptor_Settings),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp)
                    .clickable { onEvent(DescriptorViewModel.Event.AllChecked) },
            ) {
                TriStateCheckbox(
                    state = state.allState,
                    onClick = { onEvent(DescriptorViewModel.Event.AllChecked) },
                )
                Text(
                    stringResource(Res.string.AddDescriptor_AutoRun),
                    modifier = Modifier.padding(start = 16.dp),
                )
            }

            when (OrganizationConfig.testDisplayMode) {
                TestDisplayMode.Regular -> TestItems(descriptor, state.tests, onEvent)
                TestDisplayMode.WebsitesOnly -> WebsiteItems(state.tests)
            }

            if (descriptor.source is Descriptor.Source.Installed) {
                InstalledDescriptorActionsView(
                    descriptor = descriptor.source.value,
                    onEvent = onEvent,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun TestItems(
    descriptor: Descriptor,
    tests: List<SelectableItem<NetTest>>,
    onEvent: (DescriptorViewModel.Event) -> Unit,
) {
    tests.forEach { netTestItem ->
        val test = netTestItem.item
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onEvent(
                        DescriptorViewModel.Event.TestChecked(test, !netTestItem.isSelected),
                    )
                }
                .padding(start = 32.dp, top = 4.dp),
        ) {
            Checkbox(
                checked = netTestItem.isSelected,
                onCheckedChange = {
                    onEvent(DescriptorViewModel.Event.TestChecked(test, it))
                },
            )
            Icon(
                painterResource(
                    test.test.iconRes ?: descriptor.icon ?: Res.drawable.ooni_empty_state,
                ),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 16.dp, end = 8.dp)
                    .size(24.dp),
            )
            Text(
                if (test.test is TestType.Experimental) {
                    test.test.name
                } else {
                    stringResource(test.test.labelRes)
                },
            )
        }
    }
}

@Composable
private fun WebsiteItems(tests: List<SelectableItem<NetTest>>) {
    val websites = tests
        .map { it.item }
        .filter { it.test is TestType.WebConnectivity }
        .flatMap { it.inputs.orEmpty() }

    websites.forEach { website ->
        Text(
            website,
            Modifier.padding(start = 48.dp, top = 4.dp),
        )
    }
}
