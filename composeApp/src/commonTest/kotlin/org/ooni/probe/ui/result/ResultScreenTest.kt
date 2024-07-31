package org.ooni.probe.ui.result

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.ooni.probe.data.models.TestResult
import kotlin.test.Test
import kotlin.test.assertEquals

class ResultScreenTest {
    @Test
    fun showResult() =
        runComposeUiTest {
            val result = TestResult(TestResult.Id("ABCDEF"))
            setContent {
                ResultScreen(
                    state = ResultViewModel.State(result),
                    onEvent = {},
                )
            }

            onNodeWithText(result.id.value).assertExists()
        }

    @Test
    fun pressBack() =
        runComposeUiTest {
            val events = mutableListOf<ResultViewModel.Event>()
            val result = TestResult(TestResult.Id("ABCDEF"))
            setContent {
                ResultScreen(
                    state = ResultViewModel.State(result),
                    onEvent = events::add,
                )
            }

            onNodeWithContentDescription("Back").performClick()
            assertEquals(1, events.size)
            assertEquals(ResultViewModel.Event.BackClicked, events.first())
        }
}
