package org.ooni.engine.models

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.shared.toLocalDateTime

/**
 * This class represents the response from a fetch request to the OONI API.
 *
 * @see [https://github.com/ooni/spec/blob/master/backends/bk-005-ooni-run-v2.md]
 */
@Serializable
data class OONIRunDescriptor(
    @SerialName("oonirun_link_id") val oonirunLinkId: String,
    @SerialName("name") val name: String,
    @SerialName("short_description") val shortDescription: String,
    @SerialName("description") val description: String,
    @SerialName("author") val author: String,
    @SerialName("nettests") val netTests: List<OONINetTest>,
    @SerialName("name_intl") val nameIntl: Map<String, String>?,
    @SerialName("short_description_intl") val shortDescriptionIntl: Map<String, String>?,
    @SerialName("description_intl") val descriptionIntl: Map<String, String>?,
    @SerialName("icon") val icon: String?,
    @SerialName("color") val color: String?,
    @SerialName("animation") val animation: String? = null,
    @SerialName("expiration_date") val expirationDate: Instant,
    @SerialName("date_created") val dateCreated: Instant,
    @SerialName("date_updated") val dateUpdated: Instant,
    @SerialName("is_expired") val isExpired: Boolean,
    @SerialName("revision") val revision: Long,
)

fun OONIRunDescriptor.toModel() =
    InstalledTestDescriptorModel(
        id = InstalledTestDescriptorModel.Id(oonirunLinkId),
        revision = revision,
        name = name,
        shortDescription = shortDescription,
        description = description,
        author = author,
        netTests = netTests.map { NetTest.fromOONI(it) },
        nameIntl = nameIntl,
        shortDescriptionIntl = shortDescriptionIntl,
        descriptionIntl = descriptionIntl,
        icon = icon,
        color = color,
        animation = animation,
        expirationDate = expirationDate.toLocalDateTime(),
        dateCreated = dateCreated.toLocalDateTime(),
        dateUpdated = dateUpdated.toLocalDateTime(),
        autoUpdate = true,
    )
