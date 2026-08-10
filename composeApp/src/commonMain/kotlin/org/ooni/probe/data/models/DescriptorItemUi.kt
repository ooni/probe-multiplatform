package org.ooni.probe.data.models

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalDateTime.Companion.Format
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
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
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.shared.InstalledDescriptorIcons
import org.ooni.probe.shared.hexToColor
import org.ooni.probe.shared.stringMonthArrayResource

/**
 * UI presentation members for [DescriptorItem]. These live in composeApp because they use Compose
 * resources / graphics and the flavor-aware [getCurrent] locale lookup. The core [DescriptorItem]
 * in probeCore has none of these.
 */
val DescriptorItem.title: () -> String
    get() = { descriptor.nameIntl?.getCurrent() ?: descriptor.name }

val DescriptorItem.shortDescription: () -> String?
    get() = { descriptor.shortDescriptionIntl?.getCurrent() ?: descriptor.shortDescription }

val DescriptorItem.description: () -> String?
    get() = { descriptor.descriptionIntl?.getCurrent() ?: descriptor.description }

val DescriptorItem.metadata: @Composable () -> String?
    get() = {
        val monthNames = stringMonthArrayResource()
        val formattedDate = { date: LocalDateTime? -> date?.format(dateTimeFormat(monthNames)) }
        formattedDate(descriptor.dateCreated)?.let { formattedDateCreated ->
            stringResource(
                Res.string.Dashboard_Runv2_Overview_Description,
                descriptor.author.orEmpty(),
                formattedDateCreated,
            ) + ". " +
                formattedDate(descriptor.dateUpdated)?.let {
                    stringResource(Res.string.Dashboard_Runv2_Overview_LastUpdated, it)
                }
        }
    }

val DescriptorItem.runLink: String
    get() = descriptor.runLink

val DescriptorItem.icon: DrawableResource?
    get() = descriptor.icon?.let(InstalledDescriptorIcons::getIconFromValue)

val DescriptorItem.color: Color?
    get() = descriptor.color?.hexToColor()

val DescriptorItem.animation: Animation?
    get() = descriptor.icon?.let { determineAnimation(it) }
        ?: descriptor.animation?.let(Animation::fromFileName)

val DescriptorItem.dataUsage: @Composable () -> String?
    get() = { if (descriptor.isOoniDescriptor) stringResource(descriptor.getDataUsage()) else null }

fun Descriptor.getDataUsage(): StringResource =
    when (OoniTest.fromId(this.id.value)) {
        OoniTest.Websites -> Res.string.websites_datausage
        OoniTest.InstantMessaging -> Res.string.small_datausage
        OoniTest.Circumvention -> Res.string.small_datausage
        OoniTest.Performance -> Res.string.performance_datausage
        OoniTest.Experimental -> Res.string.TestResults_NotAvailable
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
