package org.ooni.probe.data.models

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalDateTime.Companion.Format
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ooniprobe.composeapp.generated.resources.Dashboard_Runv2_Overview_Description
import ooniprobe.composeapp.generated.resources.Dashboard_Runv2_Overview_LastUpdated
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.TestResults_NotAvailable
import ooniprobe.composeapp.generated.resources.performance_datausage
import ooniprobe.composeapp.generated.resources.small_datausage
import ooniprobe.composeapp.generated.resources.test_circumvention
import ooniprobe.composeapp.generated.resources.test_experimental
import ooniprobe.composeapp.generated.resources.test_instant_messaging
import ooniprobe.composeapp.generated.resources.test_performance
import ooniprobe.composeapp.generated.resources.test_websites
import ooniprobe.composeapp.generated.resources.websites_datausage
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.engine.models.SummaryType
import org.ooni.probe.data.TestDescriptor
import org.ooni.probe.shared.InstalledDescriptorIcons
import org.ooni.probe.shared.hexToColor
import org.ooni.probe.shared.stringMonthArrayResource
import org.ooni.probe.shared.toEpoch

enum class OoniTest(
    val id: Long,
    val key: String,
) {
    WEBSITES(10470L, "websites"),
    INSTANT_MESSAGING(10471L, "instant_messaging"),
    CIRCUMVENTION(10472L, "circumvention"),
    PERFORMANCE(10473L, "performance"),
    EXPERIMENTAL(10474L, "experimental"),
    ;

    companion object {
        private val map = entries.associateBy(OoniTest::id)

        fun fromId(id: Long) = map[id]

        fun isValidId(id: Long): Boolean = entries.any { it.id == id }
    }
}

@Serializable
data class InstalledTestDescriptorModel(
    val id: Id,
    val revision: Long,
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

    val isDefaultTestDescriptor get() = OoniTest.isValidId(id.value.toLongOrNull() ?: -1L)

    val key get() = Key(id, revision)

    val previousRevisions
        get() = if (revision <= 1) emptyList() else (1 until revision).toList().reversed()
}

fun InstalledTestDescriptorModel.toDescriptor(updateStatus: UpdateStatus = UpdateStatus.Unknown) =
    Descriptor(
        name = name,
        title = { nameIntl?.getCurrent() ?: name },
        shortDescription = { shortDescriptionIntl?.getCurrent() ?: shortDescription },
        description = { descriptionIntl?.getCurrent() ?: description },
        metadata = {
            val monthNames = stringMonthArrayResource()
            val formattedDate = { date: LocalDateTime? -> date?.format(dateTimeFormat(monthNames)) }
            formattedDate(dateCreated)?.let { formattedDateCreated ->
                stringResource(
                    Res.string.Dashboard_Runv2_Overview_Description,
                    author.orEmpty(),
                    formattedDateCreated,
                ) + ". " +
                    formattedDate(dateUpdated)?.let {
                        stringResource(Res.string.Dashboard_Runv2_Overview_LastUpdated, it)
                    }
            }
        },
        icon = icon?.let(InstalledDescriptorIcons::getIconFromValue),
        color = color?.hexToColor(),
        animation = icon?.let { determineAnimation(it) } ?: animation?.let(Animation::fromFileName),
        dataUsage = { if (isDefaultTestDescriptor) stringResource(getDataUsage()) else null },
        expirationDate = expirationDate,
        netTests = netTests.orEmpty(),
        source = this,
        updateStatus = updateStatus,
        // In the future, this will become a DB field with a value provided by the back-end
        summaryType = SummaryType.Anomaly,
    )

fun InstalledTestDescriptorModel.getDataUsage(): StringResource =
    when (
        OoniTest
            .fromId(
                this.key.id.value
                    .toLong(),
            )?.key
    ) {
        OoniTest.WEBSITES.key -> Res.string.websites_datausage
        OoniTest.INSTANT_MESSAGING.key -> Res.string.small_datausage
        OoniTest.CIRCUMVENTION.key -> Res.string.small_datausage
        OoniTest.PERFORMANCE.key -> Res.string.performance_datausage
        OoniTest.EXPERIMENTAL.key -> Res.string.TestResults_NotAvailable
        else -> Res.string.TestResults_NotAvailable
    }

private val iconAnimationMap = mapOf(
    Res.drawable.test_websites to Animation.Websites,
    Res.drawable.test_instant_messaging to Animation.InstantMessaging,
    Res.drawable.test_circumvention to Animation.Circumvention,
    Res.drawable.test_performance to Animation.Performance,
    Res.drawable.test_experimental to Animation.Experimental,
)

private fun determineAnimation(icon: String): Animation? = iconAnimationMap[InstalledDescriptorIcons.getIconFromValue(icon)]

private fun dateTimeFormat(monthNames: List<String>) =
    Format {
        monthName(MonthNames(monthNames))
        char(' ')
        day()
        chars(", ")
        year()
    }

fun InstalledTestDescriptorModel.toDb(json: Json) =
    TestDescriptor(
        runId = id.value,
        revision = revision,
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
        auto_update = if (autoUpdate) 1 else 0,
        rejected_revision = rejectedRevision,
    )
