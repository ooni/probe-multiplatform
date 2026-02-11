package org.ooni.testing.factories

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.DescriptorItem
import org.ooni.probe.data.models.LocalizationString
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.UpdateStatus
import org.ooni.probe.shared.toLocalDateTime
import kotlin.math.absoluteValue
import kotlin.random.Random

object DescriptorFactory {
    fun buildDescriptorWithInstalled(
        name: String = "test",
        shortDescription: String? = null,
        description: String? = null,
        netTests: List<NetTest> = emptyList(),
    ) = DescriptorItem(
        descriptor = buildInstalledModel(
            name = name,
            shortDescription = shortDescription,
            description = description,
            netTests = netTests,
        ),
        updateStatus = UpdateStatus.NoNewUpdate,
        enabled = true,
    )

    fun buildInstalledModel(
        id: Descriptor.Id =
            Descriptor.Id(Random.nextLong().absoluteValue.toString()),
        revision: Long = 1,
        name: String = "Test",
        shortDescription: String? = null,
        description: String? = null,
        author: String? = null,
        netTests: List<NetTest> = emptyList(),
        nameIntl: LocalizationString? = null,
        shortDescriptionIntl: LocalizationString? = null,
        descriptionIntl: LocalizationString? = null,
        icon: String? = null,
        color: String? = null,
        animation: String? = null,
        expirationDate: LocalDateTime? = null,
        dateCreated: LocalDateTime? = now(),
        dateUpdated: LocalDateTime? = now(),
        dateInstalled: LocalDateTime? = now(),
        autoUpdate: Boolean = false,
    ) = Descriptor(
        id = id,
        revision = revision,
        name = name,
        shortDescription = shortDescription,
        description = description,
        author = author,
        netTests = netTests,
        nameIntl = nameIntl,
        shortDescriptionIntl = shortDescriptionIntl,
        descriptionIntl = descriptionIntl,
        icon = icon,
        color = color,
        animation = animation,
        expirationDate = expirationDate,
        dateCreated = dateCreated,
        dateUpdated = dateUpdated,
        dateInstalled = dateInstalled,
        autoUpdate = autoUpdate,
    )

    // Remove nanoseconds from timestamp, because we're going to lose them
    // when storing in the database in milliseconds
    private fun now() =
        Instant
            .fromEpochMilliseconds(
                Clock.System.now().toEpochMilliseconds(),
            ).toLocalDateTime()
}
