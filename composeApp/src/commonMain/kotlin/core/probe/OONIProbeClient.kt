package core.probe

import io.github.aakira.napier.Napier
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import platform.GoOONIProbeClientBridge

val laxJson = Json {ignoreUnknownKeys = true}
@Serializable
data class ApiCallValue(
    val return_value: String,
    val error: String?,
)
@Serializable
data class HTTPResponse(
    val body: String
)

class OONIProbeClient(ooniprobeClientBridge: GoOONIProbeClientBridge) {
    private val ooniprobeClientBridge = ooniprobeClientBridge

    fun doHTTPRequest(url : String, retryCount : Int) : HTTPResponse {
        val args = buildJsonArray {
            add(JsonPrimitive(url))
            add(JsonPrimitive(retryCount))
        }

        val apiCallValue = laxJson.decodeFromString<ApiCallValue>(
            ooniprobeClientBridge.apiCallWithArgs("DoHTTPRequest", args.toString())
        )
        if (apiCallValue.error != null) {
            throw Error(apiCallValue.error)
        }
        return laxJson.decodeFromString<HTTPResponse>(apiCallValue.return_value)
    }

    fun getPublicIP() : String {
        val apiCallValue = laxJson.decodeFromString<ApiCallValue>(
            ooniprobeClientBridge.apiCall("GetPublicIP")
        )
        Napier.i("getPublicIP: return_value=${apiCallValue.return_value} error=${apiCallValue.error}")
        if (apiCallValue.error != null) {
            throw Error(apiCallValue.error)
        }
        return apiCallValue.return_value
    }
}