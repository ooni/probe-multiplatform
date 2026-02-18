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
import org.ooni.engine.models.SummaryType
import org.ooni.engine.models.TestType
import org.ooni.probe.shared.InstalledDescriptorIcons
import org.ooni.probe.shared.hexToColor
import org.ooni.probe.shared.now
import org.ooni.probe.shared.stringMonthArrayResource
import kotlin.time.Duration.Companion.seconds

data class DescriptorItem(
    val descriptor: Descriptor,
    val updateStatus: UpdateStatus,
    val enabled: Boolean = true,
) {
    val name: String
        get() = descriptor.name
    val title: @Composable () -> String
        get() = { descriptor.nameIntl?.getCurrent() ?: descriptor.name }
    val shortDescription: @Composable () -> String?
        get() = { descriptor.shortDescriptionIntl?.getCurrent() ?: descriptor.shortDescription }
    val description: @Composable () -> String?
        get() = { descriptor.descriptionIntl?.getCurrent() ?: descriptor.description }
    val metadata: @Composable () -> String? = {
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
    val icon: DrawableResource?
        get() = descriptor.icon?.let(InstalledDescriptorIcons::getIconFromValue)
    val color: Color?
        get() = descriptor.color?.hexToColor()
    val animation: Animation?
        get() = descriptor.icon?.let { determineAnimation(it) }
            ?: descriptor.animation?.let(Animation::fromFileName)
    val dataUsage: @Composable () -> String?
        get() = { if (descriptor.isOoniDescriptor) stringResource(descriptor.getDataUsage()) else null }

    val netTests: List<NetTest>
        get() = descriptor.netTests
    val longRunningTests: List<NetTest>
        get() = descriptor.longRunningTests
    val summaryType: SummaryType = SummaryType.Anomaly

    val isExpired
        get() = descriptor.expirationDate != null && descriptor.expirationDate < LocalDateTime.now()

    val updatedDescriptor
        get() = (updateStatus as? UpdateStatus.Updatable)?.updatedDescriptor

    val key: String
        get() {
            val descriptorId = descriptor.id.value
            return if (isDefault()) {
                OoniTest.fromId(descriptorId)?.key ?: descriptorId
            } else {
                descriptorId
            }
        }
    val allTests: List<NetTest>
        get() = netTests + longRunningTests

    val estimatedDuration
        get() = allTests
            .sumOf { it.test.runtime(it.inputs).inWholeSeconds }
            .seconds

    val isWebConnectivityOnly
        get() =
            allTests.size == 1 && allTests.first().test == TestType.WebConnectivity

    val runLink: String
        get() = descriptor.runLink

    val settingsPrefix: String?
        get() = if (isDefault()) null else descriptor.id.value

    fun isDefault(): Boolean = descriptor.isOoniDescriptor

    companion object {
        val SORT_COMPARATOR =
            compareByDescending<DescriptorItem> { !it.isDefault() }
                .thenBy { it.isExpired }
                .thenByDescending { it.descriptor.dateInstalled }
                .thenBy { it.descriptor.id.value }
    }
}

fun List<DescriptorItem>.notExpired() = filter { !it.isExpired }

// Extension function to convert the new Descriptor (formerly Descriptor) to DescriptorItem
fun Descriptor.toDescriptorItem(updateStatus: UpdateStatus = UpdateStatus.Unknown) =
    DescriptorItem(
        descriptor = this,
        updateStatus = updateStatus,
    )

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
