package org.ooni.probe.data.models

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.DrawableResource
import org.ooni.engine.models.SummaryType
import org.ooni.engine.models.TestType
import org.ooni.probe.shared.now
import kotlin.time.Duration.Companion.seconds

data class Descriptor(
    val name: String,
    val title: @Composable () -> String,
    val shortDescription: @Composable () -> String?,
    val description: @Composable () -> String?,
    val metadata: @Composable () -> String? = { null },
    val icon: DrawableResource?,
    val color: Color?,
    val animation: Animation?,
    val dataUsage: @Composable () -> String?,
    val expirationDate: LocalDateTime?,
    val netTests: List<NetTest>,
    val longRunningTests: List<NetTest> = emptyList(),
    val source: InstalledTestDescriptorModel,
    val updateStatus: UpdateStatus,
    val enabled: Boolean = true,
    val summaryType: SummaryType,
) {
    val isExpired get() = expirationDate != null && expirationDate < LocalDateTime.now()

    val updatedDescriptor
        get() = (updateStatus as? UpdateStatus.Updatable)?.updatedDescriptor

    val key: String
        get() {
            val descriptorId = source.id.value
            return if (isDefault()) {
                OoniTest.fromId(descriptorId)?.key ?: descriptorId
            } else {
                descriptorId
            }
        }
    val allTests get() = netTests + longRunningTests

    val estimatedDuration
        get() = allTests
            .sumOf { it.test.runtime(it.inputs).inWholeSeconds }
            .seconds

    val isWebConnectivityOnly
        get() =
            allTests.size == 1 && allTests.first().test == TestType.WebConnectivity

    val runLink get() = source.runLink

    val settingsPrefix: String?
        get() = if (isDefault()) null else source.id.value

    fun isDefault(): Boolean = source.isOoniDescriptor

    companion object {
        val SORT_COMPARATOR =
            compareByDescending<Descriptor> { !it.isDefault() }
                .thenBy { it.isExpired }
                .thenByDescending { it.source.dateInstalled }
                .thenBy { it.source.id.value }
    }
}

fun List<Descriptor>.notExpired() = filter { !it.isExpired }
