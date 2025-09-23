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
    val source: Source,
    val updateStatus: UpdateStatus,
    val enabled: Boolean = true,
    val summaryType: SummaryType,
) {
    sealed interface Source {
        data class Default(
            val value: DefaultTestDescriptor,
        ) : Source

        data class Installed(
            val value: InstalledTestDescriptorModel,
        ) : Source
    }

    val isExpired get() = expirationDate != null && expirationDate < LocalDateTime.now()

    val updatedDescriptor
        get() = (updateStatus as? UpdateStatus.Updatable)?.updatedDescriptor

    val key: String
        get() = when (source) {
            is Source.Default -> name
            is Source.Installed -> source.value.key.id.value
        }

    val allTests get() = netTests + longRunningTests

    val estimatedDuration
        get() = allTests
            .sumOf { it.test.runtime(it.inputs).inWholeSeconds }
            .seconds

    val isWebConnectivityOnly get() =
        allTests.size == 1 && allTests.first().test == TestType.WebConnectivity

    val settingsPrefix: String?
        get() = when (isDefaultDescriptor()) {
            true -> null
            else -> (source as Source.Installed)
                .value.id.value
                .toString()
        }

    fun isDefaultDescriptor(): Boolean =
        when (source) {
            is Source.Default -> true
            is Source.Installed -> source.value.isDefaultTestDescriptor
        }

    fun isInstalledNonDefaultDescriptor(): Boolean =
        when (source) {
            is Source.Installed -> !source.value.isDefaultTestDescriptor
            else -> false
        }
}

fun List<Descriptor>.notExpired() = filter { !it.isExpired }
