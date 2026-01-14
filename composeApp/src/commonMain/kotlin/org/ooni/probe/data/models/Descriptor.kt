package org.ooni.probe.data.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.TestDescriptor
import org.ooni.probe.shared.now
import org.ooni.probe.shared.toEpoch

@Serializable
data class Descriptor(
    val id: Id,
    val revision: Long,
    val name: String,
    val shortDescription: String?,
    val description: String?,
    val author: String?,
    val netTests: List<NetTest>,
    val longRunningTests: List<NetTest> = emptyList(),
    val nameIntl: LocalizationString?,
    val shortDescriptionIntl: LocalizationString?,
    val descriptionIntl: LocalizationString?,
    val icon: String?,
    val color: String?,
    val animation: String?,
    val expirationDate: LocalDateTime?,
    val dateCreated: LocalDateTime?,
    val dateUpdated: LocalDateTime?,
    val dateInstalled: LocalDateTime?,
    val rejectedRevision: Long? = null,
    val autoUpdate: Boolean,
) {
    @Serializable
    data class Id(
        val value: String,
    )

    @Serializable
    data class Key(
        val id: Id,
        val revision: Long,
    )

    val isOoniDescriptor get() = OoniTest.isValidId(id.value)

    val key get() = Key(id, revision)

    val previousRevisions
        get() = if (revision <= 1) emptyList() else (1 until revision).toList().reversed()

    val isExpired get() = expirationDate != null && expirationDate < LocalDateTime.now()

    val runLink get() = "${OrganizationConfig.ooniRunDashboardUrl}/v2/${id.value}"
}

fun Descriptor.toDb(json: Json) =
    TestDescriptor(
        runId = id.value,
        revision = revision,
        name = name,
        short_description = shortDescription,
        description = description,
        author = author,
        nettests = netTests
            .map { it.toOONI() }
            .let { json.encodeToString(it) },
        name_intl = json.encodeToString(nameIntl),
        short_description_intl = json.encodeToString(shortDescriptionIntl),
        description_intl = json.encodeToString(descriptionIntl),
        icon = icon,
        color = color,
        animation = animation,
        expiration_date = expirationDate?.toEpoch(),
        date_created = dateCreated?.toEpoch(),
        date_updated = dateUpdated?.toEpoch(),
        date_installed = dateInstalled?.toEpoch(),
        auto_update = if (autoUpdate) 1 else 0,
        rejected_revision = rejectedRevision,
    )
