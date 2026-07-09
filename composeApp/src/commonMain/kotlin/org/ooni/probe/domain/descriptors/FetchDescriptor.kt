package org.ooni.probe.domain.descriptors

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.ooni.engine.Engine.MkException
import org.ooni.engine.models.Failure
import org.ooni.engine.models.OONIRunDescriptor
import org.ooni.engine.models.Result
import org.ooni.engine.models.Success
import org.ooni.engine.models.toModel
import org.ooni.passport.PassportBridge.KeyValue
import org.ooni.passport.models.PassportException
import org.ooni.passport.models.PassportHttpResponse
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.ProxyOption
import org.ooni.probe.domain.credentials.CredentialsConstants

class FetchDescriptor(
    private val passportGet: (
        url: String,
        headers: List<KeyValue>,
        query: List<KeyValue>,
        proxy: String?,
        timeout: Float?,
    ) -> Result<PassportHttpResponse, PassportException>,
    private val getProxyOption: () -> Flow<ProxyOption>,
    private val json: Json,
) {
    suspend operator fun invoke(descriptorId: Descriptor.Id): Result<Descriptor?, MkException> {
        val proxy = getProxyOption().first().value.takeIf { it.isNotEmpty() }
        return passportGet(
            "${OrganizationConfig.ooniApiBaseUrl}/api/v2/oonirun/links/${descriptorId.value}",
            emptyList(),
            emptyList(),
            proxy,
            CredentialsConstants.HTTP_TIMEOUT_SECONDS,
        ).mapError { MkException(it) }
            .flatMap { response ->
                if (!response.isSuccessful) {
                    return@flatMap Failure(
                        MkException(Throwable("Failed to fetch descriptor (status=${response.statusCode})")),
                    )
                }
                Success(
                    response.bodyText?.let {
                        try {
                            json.decodeFromString<OONIRunDescriptor>(it).toModel()
                        } catch (e: SerializationException) {
                            Logger.e(e) { "Failed to decode descriptor ${descriptorId.value}" }
                            null
                        } catch (e: IllegalArgumentException) {
                            Logger.e(e) { "Failed to decode descriptor ${descriptorId.value}" }
                            null
                        }
                    } ?: throw MkException(Throwable("Failed to fetch descriptor")),
                )
            }
    }
}
