package org.ooni.probe.data.models

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.ooni.probe.shared.InstalledDescriptorIcons
import org.ooni.probe.shared.hexToColor
import org.ooni.probe.shared.now

@Serializable
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
    val revisions: List<String>? = emptyList(),
    val autoUpdate: Boolean,
) {
    @Serializable
    data class Id(
        val value: Long,
    )

    val isExpired get() = expirationDate != null && expirationDate < LocalDateTime.now()

    fun shouldUpdate(other: InstalledTestDescriptorModel): Boolean {
        return (other.dateUpdated?.compareTo(dateUpdated ?: other.dateUpdated) ?: 0) > 0
    }
}

fun InstalledTestDescriptorModel.toDescriptor() =
    Descriptor(
        name = name,
        title = { nameIntl?.getCurrent() ?: name },
        shortDescription = { shortDescriptionIntl?.getCurrent() ?: shortDescription },
        description = { descriptionIntl?.getCurrent() ?: description },
        icon = icon?.let(InstalledDescriptorIcons::getIconFromValue),
        color = color?.hexToColor(),
        animation = animation,
        dataUsage = { null },
        expirationDate = expirationDate,
        netTests = netTests.orEmpty(),
        source = Descriptor.Source.Installed(this),
    )
