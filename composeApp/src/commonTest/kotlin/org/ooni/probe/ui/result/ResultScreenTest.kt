package org.ooni.probe.ui.result

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.ooni.probe.data.models.ResultItem
import org.ooni.testing.factories.DescriptorFactory
import org.ooni.testing.factories.ResultModelFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class ResultScreenTest {
    @Test
    fun showResult() =
        runComposeUiTest {
            val item = ResultItem(
                result = ResultModelFactory.build(),
                network = null,
                descriptor = DescriptorFactory.buildDescriptorWithInstalled(),
                measurements = emptyList(),
            )
            var title: String? = null
            setContent {
                ResultScreen(
                    state = ResultViewModel.State(item),
                    onEvent = {},
                )

                title = item.descriptor.title()
            }

            onNodeWithText(title!!).assertExists()
        }

    @Test
    fun pressBack() =
        runComposeUiTest {
            val events = mutableListOf<ResultViewModel.Event>()
            val item = ResultItem(
                result = ResultModelFactory.build(),
                network = null,
                descriptor = DescriptorFactory.buildDescriptorWithInstalled(),
                measurements = emptyList(),
            )
            setContent {
                ResultScreen(
                    state = ResultViewModel.State(item),
                    onEvent = events::add,
                )
            }

            onNodeWithContentDescription("Back").performClick()
            assertEquals(1, events.size)
            assertEquals(ResultViewModel.Event.BackClicked, events.first())
        }
}
