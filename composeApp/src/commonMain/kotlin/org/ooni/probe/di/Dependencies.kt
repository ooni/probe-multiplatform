package org.ooni.probe.di

import kotlinx.serialization.json.Json
import org.ooni.engine.Engine
import org.ooni.engine.OonimkallBridge
import org.ooni.probe.data.models.TestResult
import org.ooni.probe.shared.PlatformInfo
import org.ooni.probe.ui.dashboard.DashboardViewModel
import org.ooni.probe.ui.result.ResultViewModel
import org.ooni.probe.ui.results.ResultsViewModel

class Dependencies(
    val platformInfo: PlatformInfo,
    private val oonimkallBridge: OonimkallBridge,
    private val baseFileDir: String,
    private val cacheDir: String,
) {
    // Data

    private val json by lazy {
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }
    }

    // Engine

    private val engine by lazy { Engine(oonimkallBridge, json, baseFileDir , cacheDir) }

    // ViewModels

    val dashboardViewModel get() = DashboardViewModel(engine)

    fun resultsViewModel(goToResult: (TestResult.Id) -> Unit) = ResultsViewModel(goToResult)

    fun resultViewModel(
        resultId: TestResult.Id,
        onBack: () -> Unit,
    ) = ResultViewModel(resultId, onBack)
}
