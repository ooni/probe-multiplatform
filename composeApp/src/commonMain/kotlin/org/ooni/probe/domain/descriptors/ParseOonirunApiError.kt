package org.ooni.probe.domain.descriptors

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Maps a non-2xx oonirun/ooniauth response to a typed [OonirunApiError], since the service does
 * not return a uniform error body shape (see [OonirunApiError] for the shapes handled).
 */
fun parseOonirunApiError(
    statusCode: Int,
    bodyText: String?,
    json: Json,
): OonirunApiError {
    val root = bodyText
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { json.parseToJsonElement(it) }.getOrNull() } as? JsonObject

    fun stringDetail() = (root?.get("detail") as? JsonPrimitive)?.contentOrNull ?: bodyText.orEmpty()

    return when (statusCode) {
        400 -> OonirunApiError.BadRequest(stringDetail())
        401 -> OonirunApiError.Unauthorized(stringDetail())
        403 -> OonirunApiError.Forbidden(stringDetail())
        404 -> OonirunApiError.NotFound
        422 -> when (val detail = root?.get("detail")) {
            is JsonObject ->
                OonirunApiError.InvalidNetTests(
                    (detail["error"] as? JsonPrimitive)?.contentOrNull ?: "Invalid nettest configuration",
                )
            is JsonArray ->
                OonirunApiError.ValidationErrors(
                    detail.mapNotNull { entry ->
                        (entry as? JsonObject)?.get("msg")?.jsonPrimitive?.contentOrNull
                    },
                )
            else -> OonirunApiError.Unknown(statusCode, bodyText)
        }
        429 -> OonirunApiError.RateLimited
        else -> OonirunApiError.Unknown(statusCode, bodyText)
    }
}
