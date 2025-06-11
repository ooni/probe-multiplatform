package org.ooni.probe.domain

import okio.Path
import okio.Path.Companion.toPath
import org.ooni.probe.data.models.ResultFilter
import org.ooni.probe.data.models.ResultModel

class DeleteResults(
    private val deleteResultsByFilter: suspend (ResultFilter) -> Unit,
    private val deleteMeasurementsWithoutResult: suspend () -> Unit,
    private val deleteNetworksWithoutResult: suspend () -> Unit,
    private val deleteAllResultsFromDatabase: suspend () -> Unit,
    private val deleteResultsByIdsFromDatabase: suspend (List<ResultModel.Id>) -> Unit,
    private val deleteFiles: suspend (Path) -> Unit,
) {
    suspend fun byFilter(filter: ResultFilter) {
        if (filter.isAll) {
            all()
        } else {
            deleteResultsByFilter(filter)
            deleteMeasurementsWithoutResult()
            deleteNetworksWithoutResult()
        }
    }

    suspend fun byIds(ids: List<ResultModel.Id>) {
        if (ids.isEmpty()) return
        deleteResultsByIdsFromDatabase(ids)
        deleteMeasurementsWithoutResult()
        deleteNetworksWithoutResult()
    }

    suspend fun all() {
        deleteAllResultsFromDatabase()
        deleteFiles("Measurement".toPath())
    }
}
