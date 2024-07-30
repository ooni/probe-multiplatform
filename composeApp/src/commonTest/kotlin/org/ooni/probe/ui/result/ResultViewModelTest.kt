package org.ooni.probe.ui.result

import org.ooni.probe.data.models.TestResult
import kotlin.test.Test
import kotlin.test.assertTrue

class ResultViewModelTest {
    @Test
    fun backClicked() {
        var backPressed = false

        val viewModel =
            ResultViewModel(
                resultId = TestResult.Id("1234"),
                onBack = { backPressed = true },
            )

        viewModel.onEvent(ResultViewModel.Event.BackClicked)
        assertTrue(backPressed)
    }
}
