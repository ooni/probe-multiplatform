package org.ooni.probe.ui.results

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlinx.datetime.LocalDate
import org.ooni.probe.data.models.ResultListItem
import org.ooni.probe.ui.result.ResultScreen
import org.ooni.probe.ui.result.ResultViewModel
import org.ooni.testing.factories.NetworkModelFactory
import org.ooni.testing.factories.ResultModelFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class ResultsScreenTest {
    @Test
    fun showResults() = runComposeUiTest {
        val item = ResultListItem(
            result = ResultModelFactory.build(),
            network = NetworkModelFactory.build(),
            measurementsCount = 4
        )
        setContent {
            ResultsScreen(
                state = ResultsViewModel.State(
                    results = mapOf(
                        LocalDate(2024, 1, 1) to listOf(item)
                    ),
                    isLoading = false
                ),
                onEvent = {},
            )
        }

        onNodeWithText("2024-01").assertExists()
        onNodeWithText(item.result.testGroupName!!).assertExists()
        onNodeWithText(item.network!!.networkName!!).assertExists()
    }

    @Test
    fun recordClick() = runComposeUiTest {
        val events = mutableListOf<ResultsViewModel.Event>()
        val item = ResultListItem(
            result = ResultModelFactory.build(),
            network = NetworkModelFactory.build(),
            measurementsCount = 4
        )
        setContent {
            ResultsScreen(
                state = ResultsViewModel.State(
                    results = mapOf(
                        LocalDate(2024, 1, 1) to listOf(item)
                    ),
                    isLoading = false
                ),
                onEvent = events::add,
            )
        }

        onNodeWithText(item.result.testGroupName!!).performClick()
        assertEquals(1, events.size)
        assertEquals(ResultsViewModel.Event.ResultClick(item), events.first())
    }
}
