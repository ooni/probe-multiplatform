package org.ooni.probe.domain

import kotlinx.serialization.json.Json
import org.ooni.engine.Engine.MkException
import org.ooni.engine.models.OONIRunDescriptor
import org.ooni.engine.models.Result
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.toModel
import org.ooni.probe.data.models.InstalledTestDescriptorModel

class FetchDescriptor(
    private val engineHttpDo: suspend (method: String, url: String, taskOrigin: TaskOrigin) -> Result<String?, MkException>,
    private val json: Json,
) {
    suspend operator fun invoke(descriptorId: String): Result<InstalledTestDescriptorModel?, MkException> {
        return engineHttpDo(
            "GET",
            "https://api.dev.ooni.io/api/v2/oonirun/links/$descriptorId",
            TaskOrigin.OoniRun,
        ).map { result ->
            result?.let {
                json.decodeFromString<OONIRunDescriptor>(it).toModel()
            } ?: throw MkException(Throwable("Failed to fetch descriptor"))
        }
    }
}
