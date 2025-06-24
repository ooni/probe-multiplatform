package org.ooni.probe.ui.result

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.ooni.probe.data.models.MeasurementModel
import org.ooni.probe.data.models.ResultItem
import org.ooni.probe.data.models.ResultModel
import org.ooni.testing.factories.DescriptorFactory
import org.ooni.testing.factories.ResultModelFactory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResultViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher(TestCoroutineScheduler())

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun backClicked() =
        runTest(dispatcher) {
            var backPressed = false
            val viewModel = buildViewModel(onBack = { backPressed = true })

            viewModel.onEvent(ResultViewModel.Event.BackClicked)
            assertTrue(backPressed)
        }

    @Test
    fun getResult() =
        runTest(dispatcher) {
            val item = ResultItem(
                result = ResultModelFactory.build(),
                network = null,
                descriptor = DescriptorFactory.buildDescriptorWithInstalled(),
                measurements = emptyList(),
                testKeys = emptyList(),
            )
            val viewModel = buildViewModel(getResult = { flowOf(item) })

            assertEquals(item, viewModel.state.first().result)
        }

    private fun buildViewModel(
        resultId: ResultModel.Id = ResultModel.Id(1234),
        onBack: () -> Unit = {},
        goToMeasurement: (MeasurementModel.Id) -> Unit = {},
        goToMeasurementRaw: (MeasurementModel.Id) -> Unit = {},
        getResult: (ResultModel.Id) -> Flow<ResultItem?> = { flowOf(null) },
        markResultAsViewed: (ResultModel.Id) -> Unit = {},
    ) = ResultViewModel(
        resultId = resultId,
        onBack = onBack,
        goToMeasurement = goToMeasurement,
        goToMeasurementRaw = goToMeasurementRaw,
        goToUpload = {},
        goToDashboard = {},
        getResult = getResult,
        getCurrentRunBackgroundState = emptyFlow(),
        markResultAsViewed = markResultAsViewed,
        startBackgroundRun = {},
    )
}
