package org.ooni.testing.factories

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.ooni.probe.data.models.LocalizationString
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.TestDescriptorModel
import kotlin.math.absoluteValue
import kotlin.random.Random

object TestDescriptorModelFactory {
    fun build(
        id: TestDescriptorModel.Id = TestDescriptorModel.Id(Random.nextLong().absoluteValue),
        name: String? = "Test",
        shortDescription: String? = null,
        description: String? = null,
        author: String? = null,
        netTests: List<NetTest>? = null,
        nameIntl: LocalizationString? = null,
        shortDescriptionIntl: LocalizationString? = null,
        descriptionIntl: LocalizationString? = null,
        icon: String? = null,
        color: String? = null,
        animation: String? = null,
        expirationDate: Instant? = null,
        // Remove nanoseconds from timestamp, because we're going to lose them
        // when storing in the database in milliseconds
        dateCreated: Instant? =
            Instant.fromEpochMilliseconds(
                Clock.System.now().toEpochMilliseconds(),
            ),
        dateUpdated: Instant? = null,
        revision: String? = null,
        previousRevision: String? = null,
        isExpired: Boolean = false,
        autoUpdate: Boolean = false,
    ) =
        TestDescriptorModel(
            id = id,
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
            revision = revision,
            previousRevision = previousRevision,
            isExpired = isExpired,
            autoUpdate = autoUpdate,
        )
}
