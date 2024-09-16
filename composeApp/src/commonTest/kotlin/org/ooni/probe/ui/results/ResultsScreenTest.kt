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
import org.ooni.probe.data.models.ResultListItem
import org.ooni.testing.factories.DescriptorFactory
import org.ooni.testing.factories.NetworkModelFactory
import org.ooni.testing.factories.ResultModelFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class ResultsScreenTest {
    @Test
    fun showResults() =
        runComposeUiTest {
            val item = buildItem()
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

                title = item.descriptor.title()
            }

            onNodeWithText("January 2024").assertExists()
            onNodeWithText(title!!).assertExists()
            onNodeWithText(item.network!!.networkName!!).assertExists()
        }

    @Test
    fun recordClick() =
        runComposeUiTest {
            val events = mutableListOf<ResultsViewModel.Event>()
            val item = buildItem()
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

                title = item.descriptor.title()
            }

            onNodeWithText(title!!).performClick()
            assertEquals(ResultsViewModel.Event.ResultClick(item), events.first())
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
                assertEquals(ResultsViewModel.Event.DeleteAllClick, events.first())
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
        ResultListItem(
            result = ResultModelFactory.build(),
            descriptor = DescriptorFactory.buildDescriptorWithInstalled(),
            network = NetworkModelFactory.build(),
            measurementsCount = 4,
            allMeasurementsUploaded = false,
        )
}
