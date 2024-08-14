package org.ooni.probe.data.models

import kotlinx.datetime.LocalDateTime
import org.ooni.probe.shared.now

data class InstalledTestDescriptorModel(
    val id: Id,
    val name: String,
    val shortDescription: String?,
    val description: String?,
    val author: String?,
    val netTests: List<NetTest>?,
    val nameIntl: LocalizationString?,
    val shortDescriptionIntl: LocalizationString?,
    val descriptionIntl: LocalizationString?,
    val icon: String?,
    val color: String?,
    val animation: String?,
    val expirationDate: LocalDateTime?,
    val dateCreated: LocalDateTime?,
    val dateUpdated: LocalDateTime?,
    val revision: String?,
    val autoUpdate: Boolean,
) {
    data class Id(
        val value: Long,
    )

    val isExpired get() = expirationDate != null && expirationDate < LocalDateTime.now()
}
