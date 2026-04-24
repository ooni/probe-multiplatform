package org.ooni.probe.domain.credentials

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.ooni.passport.PassportGetProbeId
import org.ooni.probe.data.models.Credential

class StampMeasurement(
    private val passportGetProbeId: PassportGetProbeId,
    private val getCredential: suspend () -> Credential?,
    private val json: Json,
) {
    suspend operator fun invoke(content: String): String {
        val credential = getCredential() ?: return content
        val parsed = try {
            json.parseToJsonElement(content) as? JsonObject
        } catch (e: Exception) {
            Logger.w("StampMeasurement: cannot parse measurement JSON", e)
            null
        } ?: return content

        val probeCc = parsed["probe_cc"]?.stringValue() ?: return content
        val probeAsn = parsed["probe_asn"]?.stringValue() ?: return content

        val probeId = passportGetProbeId
            .getProbeId(
                credentialB64 = credential.credential,
                probeAsn = probeAsn,
                probeCc = probeCc,
            ).get()
            ?: run {
                Logger.w("StampMeasurement: getProbeId returned no id")
                return content
            }

        val stamped = JsonObject(parsed + ("probe_id" to JsonPrimitive(probeId)))
        return json.encodeToString(JsonObject.serializer(), stamped)
    }

    private fun JsonElement.stringValue(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content
}
