package org.ooni.probe.domain

import okio.Path
import okio.Path.Companion.toPath
import org.ooni.probe.data.models.ResultFilter

class DeleteResults(
    private val deleteResultsByFilter: suspend (ResultFilter) -> Unit,
    private val deleteMeasurementsWithoutResult: suspend () -> Unit,
    private val deleteNetworksWithoutResult: suspend () -> Unit,
    private val deleteAllResultsFromDatabase: suspend () -> Unit,
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

    suspend fun all() {
        deleteAllResultsFromDatabase()
        deleteFiles("Measurement".toPath())
    }
}
