package org.ooni.probe.di

import androidx.annotation.VisibleForTesting
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.serialization.json.Json
import org.ooni.engine.Engine
import org.ooni.engine.NetworkTypeFinder
import org.ooni.engine.OonimkallBridge
import org.ooni.engine.TaskEventMapper
import org.ooni.probe.Database
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.repositories.ResultRepository
import org.ooni.probe.domain.GetResult
import org.ooni.probe.domain.GetResults
import org.ooni.probe.shared.PlatformInfo
import org.ooni.probe.ui.dashboard.DashboardViewModel
import org.ooni.probe.ui.result.ResultViewModel
import org.ooni.probe.ui.results.ResultsViewModel

class Dependencies(
    val platformInfo: PlatformInfo,
    private val oonimkallBridge: OonimkallBridge,
    private val baseFileDir: String,
    private val cacheDir: String,
    private val databaseDriverFactory: () -> SqlDriver,
    private val networkTypeFinder: NetworkTypeFinder,
) {
    // Common

    private val backgroundDispatcher = Dispatchers.IO

    // Data

    private val json by lazy { buildJson() }
    private val database by lazy { buildDatabase(databaseDriverFactory) }
    private val resultRepository by lazy { ResultRepository(database, backgroundDispatcher) }

    // Engine

    private val taskEventMapper by lazy { TaskEventMapper(networkTypeFinder, json) }

    private val engine by lazy {
        Engine(
            bridge = oonimkallBridge,
            json = json,
            baseFilePath = baseFileDir,
            cacheDir = cacheDir,
            taskEventMapper = taskEventMapper,
            networkTypeFinder = networkTypeFinder,
            platformInfo = platformInfo,
            backgroundDispatcher = backgroundDispatcher,
        )
    }

    // Domain

    private val getResults by lazy { GetResults(resultRepository) }
    private val getResult by lazy { GetResult(resultRepository) }

    // ViewModels

    val dashboardViewModel get() = DashboardViewModel(engine)

    fun resultsViewModel(
        goToResult: (ResultModel.Id) -> Unit
    ) = ResultsViewModel(goToResult, getResults::invoke)

    fun resultViewModel(
        resultId: ResultModel.Id,
        onBack: () -> Unit,
    ) = ResultViewModel(resultId, onBack, getResult::invoke)

    companion object {
        @VisibleForTesting
        fun buildJson() =
            Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
            }

        @VisibleForTesting
        fun buildDatabase(driverFactory: () -> SqlDriver): Database = Database(driverFactory())
    }
}
