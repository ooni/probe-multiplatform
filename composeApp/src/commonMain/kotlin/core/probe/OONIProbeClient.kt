package core.probe

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import platform.OONIProbeEngine

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

class OONIProbeClient(
    ooniProbeEngine: OONIProbeEngine
) {
    private val ooniProbeEngine = ooniProbeEngine
    fun doDemoCheck() : String {
        return ooniProbeEngine.demoCheck()
    }

    fun doHTTPRequest(url : String, retryCount : Int) {
        /*
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
         */
    }

    fun getPublicIP() : String {
        /*
        val apiCallValue = laxJson.decodeFromString<ApiCallValue>(
            ooniprobeClientBridge.apiCall("GetPublicIP")
        )
        Napier.i("getPublicIP: return_value=${apiCallValue.return_value} error=${apiCallValue.error}")
        if (apiCallValue.error != null) {
            throw Error(apiCallValue.error)
        }
        return apiCallValue.return_value
         */
        return ""
    }
}