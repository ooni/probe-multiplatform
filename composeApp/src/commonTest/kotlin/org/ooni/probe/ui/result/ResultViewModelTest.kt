package org.ooni.probe.ui.result

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.ooni.probe.data.models.ResultModel
import org.ooni.testing.factories.ResultModelFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResultViewModelTest {

    @Test
    fun backClicked() {
        var backPressed = false
        val viewModel = buildViewModel(onBack = { backPressed = true })

        viewModel.onEvent(ResultViewModel.Event.BackClicked)
        assertTrue(backPressed)
    }

    @Test
    fun getResult() = runTest {
        val result = ResultModelFactory.build()
        val viewModel = buildViewModel(getResult = { flowOf(result) })

        assertEquals(result, viewModel.state.first().result)
    }

    private fun buildViewModel(
        resultId: ResultModel.Id = ResultModel.Id(1234),
        onBack: () -> Unit = {},
        getResult: (ResultModel.Id) -> Flow<ResultModel?> = { flowOf(null) }
    ) = ResultViewModel(
        resultId = resultId,
        onBack = onBack,
        getResult = getResult,
    )
}
