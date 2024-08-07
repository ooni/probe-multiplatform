package org.ooni.probe.data.models

import kotlinx.datetime.Instant

data class TestDescriptorModel(
    val id: Id,
    val name: String?,
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
    val expirationDate: Instant?,
    val dateCreated: Instant?,
    val dateUpdated: Instant?,
    val revision: String?,
    val previousRevision: String?,
    val isExpired: Boolean,
    val autoUpdate: Boolean,
) {
    data class Id(
        val value: Long,
    )
}
