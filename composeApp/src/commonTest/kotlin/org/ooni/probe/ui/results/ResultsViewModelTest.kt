package org.ooni.probe.ui.results

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.ooni.probe.data.models.ResultsStats
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResultsViewModelTest {
    @Test
    fun uploadVisibilityTracksBackgroundRunState() =
        runTest {
            val isRunning = MutableStateFlow(false)
            val viewModel = ResultsViewModel(
                goToResult = {},
                goToUpload = {},
                getResults = { flowOf(emptyList()) },
                getResultsStats = { flowOf(ResultsStats(0, 0, 0, 0)) },
                getDescriptors = { flowOf(emptyList()) },
                getNetworks = { flowOf(emptyList()) },
                deleteResultsByFilter = {},
                markAsViewed = {},
                observeRunBackgroundIsRunning = { isRunning },
            )

            assertFalse(viewModel.state.first().isTesting)

            isRunning.value = true
            assertTrue(viewModel.state.first { it.isTesting }.isTesting)

            isRunning.value = false
            assertFalse(viewModel.state.first { !it.isTesting }.isTesting)
        }
}
