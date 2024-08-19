package org.ooni.probe.ui.results

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.datetime.LocalDate
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
            val item = ResultListItem(
                result = ResultModelFactory.build(),
                descriptor = DescriptorFactory.buildDescriptorWithInstalled(),
                network = NetworkModelFactory.build(),
                measurementsCount = 4,
                allMeasurementsUploaded = false,
            )
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
            val item = ResultListItem(
                result = ResultModelFactory.build(),
                descriptor = DescriptorFactory.buildDescriptorWithInstalled(),
                network = NetworkModelFactory.build(),
                measurementsCount = 4,
                allMeasurementsUploaded = false,
            )
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
            assertEquals(1, events.size)
            assertEquals(ResultsViewModel.Event.ResultClick(item), events.first())
        }
}
