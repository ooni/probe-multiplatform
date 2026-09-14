package org.ooni.probe.ui.results

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.ooni.probe.data.models.ResultsStats
import org.ooni.probe.data.models.RunBackgroundState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResultsViewModelTest {
    @Test
    fun uploadVisibilityTracksBackgroundRunState() =
        runTest {
            val backgroundState = MutableStateFlow<RunBackgroundState>(RunBackgroundState.Idle)
            val viewModel = ResultsViewModel(
                goToResult = {},
                goToUpload = {},
                getResults = { flowOf(emptyList()) },
                getResultsStats = { flowOf(ResultsStats(0, 0, 0, 0)) },
                getDescriptors = { flowOf(emptyList()) },
                getNetworks = { flowOf(emptyList()) },
                deleteResultsByFilter = {},
                markAsViewed = {},
                observeRunBackgroundState = { backgroundState },
            )

            assertFalse(viewModel.state.first().isTesting)

            backgroundState.value = RunBackgroundState.RunningTests()
            assertTrue(viewModel.state.first { it.isTesting }.isTesting)

            backgroundState.value = RunBackgroundState.Idle
            assertFalse(viewModel.state.first { !it.isTesting }.isTesting)
        }
}
