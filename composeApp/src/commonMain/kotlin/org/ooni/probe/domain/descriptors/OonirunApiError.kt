package org.ooni.probe.domain.descriptors

import org.ooni.passport.models.PassportException

/**
 * The oonirun v2 API does not return a uniform error body - `detail` can be a bare string, an
 * `{"error": ...}` object, or a list of Pydantic field errors, and 429s use a different key
 * entirely. See [parseOonirunError] for how each shape maps to a case here.
 */
sealed class OonirunApiError(
    message: String,
) : Exception(message) {
    /** 422 `{"detail": {"error": "..."}}` - e.g. targetsName combined with inputs. */
    data class InvalidNetTests(
        val detail: String,
    ) : OonirunApiError(detail)

    /** 400 `{"detail": "..."}` - e.g. author mismatch on create. */
    data class BadRequest(
        val detail: String,
    ) : OonirunApiError(detail)

    /** 403 `{"detail": "..."}` - e.g. author mismatch on edit, or editing an expired link. */
    data class Forbidden(
        val detail: String,
    ) : OonirunApiError(detail)

    data object NotFound : OonirunApiError("OONI Run link not found")

    /** 401 - missing/invalid Bearer token or insufficient role. */
    data class Unauthorized(
        val detail: String,
    ) : OonirunApiError(detail)

    /** 422 `{"detail": [{"loc": [...], "msg": "...", "type": "..."}]}` - default field validation. */
    data class ValidationErrors(
        val messages: List<String>,
    ) : OonirunApiError(messages.joinToString())

    /** 429 `{"error": "quota exceeded"}` - shared IP-based rate limit. */
    data object RateLimited : OonirunApiError("Rate limit exceeded")

    data class Network(
        override val cause: PassportException,
    ) : OonirunApiError(cause.message ?: "Network error")

    data class Decode(
        override val cause: Throwable,
    ) : OonirunApiError(cause.message ?: "Failed to decode response")

    data class Unknown(
        val statusCode: Int,
        val bodyText: String?,
    ) : OonirunApiError("HTTP $statusCode")
}
