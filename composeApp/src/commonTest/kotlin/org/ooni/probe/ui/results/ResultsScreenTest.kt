package org.ooni.probe.ui.results

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import ooniprobe.composeapp.generated.resources.Modal_Delete
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.TestResults_Overview_NoTestsHaveBeenRun
import org.jetbrains.compose.resources.getString
import org.ooni.probe.data.models.MeasurementCounts
import org.ooni.probe.data.models.ResultListItem
import org.ooni.testing.factories.DescriptorFactory
import org.ooni.testing.factories.NetworkModelFactory
import org.ooni.testing.factories.ResultModelFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class ResultsScreenTest {
    @Test
    fun start() =
        runComposeUiTest {
            val events = mutableListOf<ResultsViewModel.Event>()
            setContent {
                ResultsScreen(
                    state = ResultsViewModel.State(results = emptyMap(), isLoading = true),
                    onEvent = events::add,
                )
            }

            assertEquals(ResultsViewModel.Event.Start, events.last())
        }

    @Test
    fun showResults() =
        runComposeUiTest {
            val item = buildItem()
            val result = item.results.first().item
            var title: String? = null

            setContent {
                ResultsScreen(
                    state = ResultsViewModel.State(
                        results = mapOf(
                            LocalDate(2024, 1, 1) to listOf(item),
                        ),
                        isLoading = false,
                    ),
                    onEvent = {},
                )

                title = result.descriptor.title()
            }

            onNodeWithText("1 January 2024").assertExists()
            onNodeWithText(title!!).assertExists()
            onNodeWithText(result.network!!.networkName!!, substring = true).assertExists()
        }

    @Test
    fun recordClick() =
        runComposeUiTest {
            val events = mutableListOf<ResultsViewModel.Event>()
            val item = buildItem()
            val result = item.results.first().item
            var title: String? = null

            setContent {
                ResultsScreen(
                    state = ResultsViewModel.State(
                        results = mapOf(
                            LocalDate(2024, 1, 1) to listOf(item),
                        ),
                        isLoading = false,
                    ),
                    onEvent = events::add,
                )

                title = result.descriptor.title()
            }

            onNodeWithText(title!!).performClick()
            assertEquals(ResultsViewModel.Event.ResultClick(result), events.last())
        }

    @Test
    fun deleteAllClick() =
        runComposeUiTest {
            val events = mutableListOf<ResultsViewModel.Event>()
            val item = buildItem()
            setContent {
                ResultsScreen(
                    state = ResultsViewModel.State(
                        results = mapOf(
                            LocalDate(2024, 1, 1) to listOf(item),
                        ),
                        isLoading = false,
                    ),
                    onEvent = events::add,
                )
            }

            runTest {
                onNodeWithContentDescription(getString(Res.string.Modal_Delete)).performClick()
                onNodeWithText(getString(Res.string.Modal_Delete)).performClick()
                assertEquals(ResultsViewModel.Event.DeleteClick, events.last())
            }
        }

    @Test
    fun emptyResults() =
        runComposeUiTest {
            setContent {
                ResultsScreen(
                    state = ResultsViewModel.State(
                        results = emptyMap(),
                        isLoading = false,
                    ),
                    onEvent = {},
                )
            }

            runTest {
                onNodeWithText(getString(Res.string.TestResults_Overview_NoTestsHaveBeenRun))
                    .assertExists()
            }
        }

    private fun buildItem() =
        RunListItem
            .aggregateResults(
                listOf(
                    ResultListItem(
                        result = ResultModelFactory.build(),
                        descriptor = DescriptorFactory.buildDescriptorWithInstalled(),
                        network = NetworkModelFactory.build(),
                        measurementCounts = MeasurementCounts(
                            done = 4,
                            failed = 0,
                            anomaly = 0,
                        ),
                        allMeasurementsUploaded = true,
                        anyMeasurementUploadFailed = false,
                        testKeys = emptyList(),
                    ),
                ),
            ).first()
}
