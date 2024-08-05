package org.ooni.probe.di

import androidx.annotation.VisibleForTesting
import kotlinx.serialization.json.Json
import org.ooni.engine.Engine
import org.ooni.engine.NetworkTypeFinder
import org.ooni.engine.OonimkallBridge
import org.ooni.engine.TaskEventMapper
import org.ooni.engine.models.NetworkType
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

    private val json by lazy { buildJson() }

    // Engine

    private val networkTypeFinder by lazy { NetworkTypeFinder { NetworkType.Unknown("") } } // TODO
    private val taskEventMapper by lazy { TaskEventMapper(networkTypeFinder, json) }
    private val engine by lazy { Engine(oonimkallBridge, json, baseFileDir, cacheDir, taskEventMapper) }

    // ViewModels

    val dashboardViewModel get() = DashboardViewModel(engine)

    fun resultsViewModel(goToResult: (TestResult.Id) -> Unit) = ResultsViewModel(goToResult)

    fun resultViewModel(
        resultId: TestResult.Id,
        onBack: () -> Unit,
    ) = ResultViewModel(resultId, onBack)

    companion object {
        @VisibleForTesting
        fun buildJson() =
            Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
            }
    }
}
