package org.ooni.engine.models

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for `POST /api/v2/oonirun/links` (create a new OONI Run v2 link).
 *
 * Deliberately excludes fields the server assigns (`oonirun_link_id`, `revision`, `date_created`,
 * `date_updated`, `is_expired`) and the app-local `animation` field, which is not part of the
 * real wire contract.
 *
 * @see [https://github.com/ooni/spec/blob/master/backends/bk-005-ooni-run-v2.md]
 */
@Serializable
data class OONIRunLinkCreateRequest(
    @SerialName("name") val name: String,
    @SerialName("name_intl") val nameIntl: Map<String, String>? = null,
    @SerialName("short_description") val shortDescription: String,
    @SerialName("short_description_intl") val shortDescriptionIntl: Map<String, String>? = null,
    @SerialName("description") val description: String,
    @SerialName("description_intl") val descriptionIntl: Map<String, String>? = null,
    // Must equal the authenticated account's email address, or the server rejects the request.
    @SerialName("author") val author: String,
    @SerialName("icon") val icon: String? = null,
    @SerialName("color") val color: String? = null,
    // Server defaults to now + 6 months when omitted.
    @SerialName("expiration_date") val expirationDate: Instant? = null,
    @SerialName("nettests") val nettests: List<OONINetTest>,
)
