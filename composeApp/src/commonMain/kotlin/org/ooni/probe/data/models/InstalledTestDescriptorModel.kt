package org.ooni.probe.data.models

import co.touchlab.kermit.Logger
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ooniprobe.composeapp.generated.resources.Dashboard_Runv2_Overview_Description
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import kotlinx.datetime.LocalDateTime.Companion.Format
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import ooniprobe.composeapp.generated.resources.Dashboard_Runv2_Overview_LastUpdatd
import ooniprobe.composeapp.generated.resources.months
import org.jetbrains.compose.resources.stringArrayResource
import org.ooni.probe.data.TestDescriptor
import org.ooni.probe.shared.InstalledDescriptorIcons
import org.ooni.probe.shared.hexToColor
import org.ooni.probe.shared.now
import org.ooni.probe.shared.toEpoch

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
        return dateUpdated != null && other.dateUpdated != null && other.dateUpdated > dateUpdated
    }
}

fun InstalledTestDescriptorModel.toDescriptor(updateStatus: UpdateStatus = UpdateStatus.Unknown) =
    Descriptor(
        name = name,
        title = { nameIntl?.getCurrent() ?: name },
        shortDescription = { shortDescriptionIntl?.getCurrent() ?: shortDescription },
        description = { descriptionIntl?.getCurrent() ?: description },
        metadata = {
            val monthNames = stringArrayResource(Res.array.months)
            val formattedDate = { date: LocalDateTime? -> date?.format(dateTimeFormat(monthNames)) }
            formattedDate(dateCreated)?.let { formattedDateCreated ->
                stringResource(
                    Res.string.Dashboard_Runv2_Overview_Description,
                    author.orEmpty(),
                    formattedDateCreated,
                ) + ". " + formattedDate(dateUpdated)?.let {
                    stringResource(Res.string.Dashboard_Runv2_Overview_LastUpdatd, it)
                }
            }
        },
        icon = icon?.let(InstalledDescriptorIcons::getIconFromValue),
        color = color?.hexToColor(),
        animation = animation?.let(Animation::fromFileName),
        dataUsage = { null },
        expirationDate = expirationDate,
        netTests = netTests.orEmpty(),
        source = Descriptor.Source.Installed(this),
        updateStatus = updateStatus,
    )

private fun dateTimeFormat(monthNames: List<String>) =
    Format {
        monthName(MonthNames(monthNames))
        char(' ')
        dayOfMonth()
        chars(" , ")
        year()
    }

fun InstalledTestDescriptorModel.toDb(json: Json): TestDescriptor {
    return TestDescriptor(
        runId = id.value,
        name = name,
        short_description = shortDescription,
        description = description,
        author = author,
        nettests = netTests
            ?.map { it.toOONI() }
            ?.let { json.encodeToString(it) },
        name_intl = json.encodeToString(nameIntl),
        short_description_intl = json.encodeToString(shortDescriptionIntl),
        description_intl = json.encodeToString(descriptionIntl),
        icon = icon,
        color = color,
        animation = animation,
        expiration_date = expirationDate?.toEpoch(),
        date_created = dateCreated?.toEpoch(),
        date_updated = dateUpdated?.toEpoch(),
        revision = try {
            json.encodeToString(revisions)
        } catch (e: Exception) {
            Logger.e(e) { "Failed to encode revisions" }
            null
        },
        previous_revision = null,
        is_expired = if (isExpired) 1 else 0,
        auto_update = if (autoUpdate) 1 else 0,
    )
}
