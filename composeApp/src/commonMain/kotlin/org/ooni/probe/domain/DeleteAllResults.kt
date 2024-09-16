package org.ooni.probe.domain

import okio.Path
import okio.Path.Companion.toPath

class DeleteAllResults(
    private val deleteAllResultsFromDatabase: suspend () -> Unit,
    private val deleteFiles: suspend (Path) -> Unit,
) {
    suspend operator fun invoke() {
        deleteAllResultsFromDatabase()
        deleteFiles("Measurement".toPath())
    }
}
