package org.ooni.testing.factories

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.DrawableResource
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.InstalledTestDescriptorModel
import org.ooni.probe.data.models.LocalizationString
import org.ooni.probe.data.models.NetTest
import kotlin.math.absoluteValue
import kotlin.random.Random

object DescriptorFactory {
    fun buildDescriptorWithInstalled(
        name: String = "test",
        title: String = "Test",
        shortDescription: String? = null,
        description: String? = null,
        icon: DrawableResource? = null,
        color: Color? = null,
        animation: String? = null,
        dataUsage: String? = null,
        netTests: List<NetTest> = emptyList(),
        longRunningTests: List<NetTest>? = null,
        installedTestDescriptorModel: InstalledTestDescriptorModel = buildInstalledModel(),
    ) = Descriptor(
        name = name,
        title = { title },
        shortDescription = { shortDescription },
        description = { description },
        icon = icon,
        color = color,
        animation = animation,
        dataUsage = { dataUsage },
        netTests = netTests,
        longRunningTests = longRunningTests,
        source = Descriptor.Source.Installed(installedTestDescriptorModel),
    )

    fun buildInstalledModel(
        id: InstalledTestDescriptorModel.Id = InstalledTestDescriptorModel.Id(Random.nextLong().absoluteValue),
        name: String = "Test",
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
        isExpired: Boolean = false,
        autoUpdate: Boolean = false,
    ) =
        InstalledTestDescriptorModel(
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
            isExpired = isExpired,
            autoUpdate = autoUpdate,
        )
}