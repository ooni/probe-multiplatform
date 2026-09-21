package org.ooni.probe.data.models

import kotlinx.datetime.LocalDateTime
import org.ooni.engine.models.SummaryType
import org.ooni.engine.models.TestType
import org.ooni.probe.shared.now
import kotlin.time.Duration.Companion.seconds

/**
 * Core (non-UI) view of a [Descriptor] used by run orchestration.
 *
 * Presentation members (localized `title`/`shortDescription`/`description`, Compose `metadata`,
 * `icon`, `color`, `animation`, `dataUsage`) live as extensions in `composeApp`
 * (`DescriptorItemUi.kt`), so this stays free of Compose/flavor dependencies.
 */
data class DescriptorItem(
    val descriptor: Descriptor,
    val updateStatus: UpdateStatus,
    val enabled: Boolean = true,
) {
    val name: String
        get() = descriptor.name

    val netTests: List<NetTest>
        get() = descriptor.netTests

    val longRunningTests: List<NetTest>
        get() = descriptor.longRunningTests

    val summaryType: SummaryType
        get() = when (OoniTest.fromId(descriptor.id.value)) {
            OoniTest.Performance -> SummaryType.Performance
            OoniTest.Experimental -> SummaryType.Simple
            OoniTest.Circumvention,
            OoniTest.InstantMessaging,
            OoniTest.Websites,
            null,
            -> SummaryType.Anomaly
        }

    val isExpired: Boolean
        get() {
            val exp = descriptor.expirationDate
            return exp != null && exp < LocalDateTime.now()
        }

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

fun Descriptor.toDescriptorItem(updateStatus: UpdateStatus = UpdateStatus.Unknown) =
    DescriptorItem(
        descriptor = this,
        updateStatus = updateStatus,
    )
