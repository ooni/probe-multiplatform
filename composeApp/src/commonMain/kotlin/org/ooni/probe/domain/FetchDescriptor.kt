package org.ooni.probe.domain

import org.ooni.engine.Engine.MkException
import org.ooni.engine.models.Result
import org.ooni.engine.models.TaskOrigin

class FetchDescriptor(
    private val engineHttpDo: suspend (method: String, url: String, taskOrigin: TaskOrigin) -> Result<String?, MkException>,
) {
    suspend operator fun invoke(descriptorId: String): Result<String?, MkException> {
        return engineHttpDo("GET", "https://api.dev.ooni.io/api/v2/oonirun/links/$descriptorId", TaskOrigin.OoniRun)
            .map { result ->
                result?.let {
                    result

                    result
                } ?: throw MkException(Throwable("Failed to fetch descriptor"))
            }
    }
}
