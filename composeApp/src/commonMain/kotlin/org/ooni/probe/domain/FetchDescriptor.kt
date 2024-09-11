package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import org.ooni.engine.Engine.MkException
import org.ooni.engine.models.OONIRunDescriptor
import org.ooni.engine.models.OONIRunRevisions
import org.ooni.engine.models.Result
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.toModel
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.InstalledTestDescriptorModel

class FetchDescriptor(
    private val engineHttpDo: suspend (method: String, url: String, taskOrigin: TaskOrigin) -> Result<String?, MkException>,
    private val json: Json,
) {
    suspend operator fun invoke(descriptorId: String): Result<InstalledTestDescriptorModel?, MkException> {
        return engineHttpDo(
            "GET",
            "${OrganizationConfig.ooniApiBaseUrl}/api/v2/oonirun/links/$descriptorId",
            TaskOrigin.OoniRun,
        ).map { result ->
            result?.let {
                try {
                    json.decodeFromString<OONIRunDescriptor>(it).toModel().copy(
                        revisions = fetchRevisions(descriptorId).get()?.revisions,
                    )
                } catch (e: Throwable) {
                    Logger.e(e) { "Failed to decode descriptor" }
                    null
                }
            } ?: throw MkException(Throwable("Failed to fetch descriptor"))
        }
    }

    private suspend fun fetchRevisions(descriptorId: String): Result<OONIRunRevisions?, MkException> {
        return engineHttpDo(
            "GET",
            "${OrganizationConfig.ooniApiBaseUrl}/api/v2/oonirun/links/$descriptorId/revisions",
            TaskOrigin.OoniRun,
        ).map { result ->
            result?.let {
                try {
                    json.decodeFromString<OONIRunRevisions>(it)
                } catch (e: Throwable) {
                    Logger.e(e) { "Failed to decode revisions" }
                    null
                }
            } ?: throw MkException(Throwable("Failed to fetch revision"))
        }
    }
}
