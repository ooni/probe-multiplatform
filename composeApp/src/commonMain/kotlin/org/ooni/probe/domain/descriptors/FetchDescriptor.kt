package org.ooni.probe.domain.descriptors

import co.touchlab.kermit.Logger
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.ooni.engine.Engine.MkException
import org.ooni.engine.models.OONIRunDescriptor
import org.ooni.engine.models.Result
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.toModel
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.Descriptor

class FetchDescriptor(
    private val engineHttpDo: suspend (method: String, url: String, taskOrigin: TaskOrigin) -> Result<String?, MkException>,
    private val json: Json,
) {
    suspend operator fun invoke(descriptorId: String): Result<Descriptor?, MkException> =
        engineHttpDo(
            "GET",
            "${OrganizationConfig.ooniApiBaseUrl}/api/v2/oonirun/links/$descriptorId",
            TaskOrigin.OoniRun,
        ).map { result ->
            result?.let {
                try {
                    json.decodeFromString<OONIRunDescriptor>(it).toModel()
                } catch (e: SerializationException) {
                    Logger.e(e) { "Failed to decode descriptor $descriptorId" }
                    null
                } catch (e: IllegalArgumentException) {
                    Logger.e(e) { "Failed to decode descriptor $descriptorId" }
                    null
                }
            } ?: throw MkException(Throwable("Failed to fetch descriptor"))
        }
}
