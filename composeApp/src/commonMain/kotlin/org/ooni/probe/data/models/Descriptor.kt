package org.ooni.probe.data.models

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.DrawableResource
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
) {
    sealed interface Source {
        data class Default(val value: DefaultTestDescriptor) : Source

        data class Installed(val value: InstalledTestDescriptorModel) : Source
    }

    val isExpired get() = expirationDate != null && expirationDate < LocalDateTime.now()

    val updatable get() = updateStatus is UpdateStatus.UpdateRejected

    val key: String
        get() = when (source) {
            is Source.Default -> name
            is Source.Installed -> source.value.id.value.toString()
        }

    val allTests get() = netTests + longRunningTests

    val estimatedDuration
        get() = allTests
            .sumOf { it.test.runtime(it.inputs).inWholeSeconds }
            .seconds
}

fun List<Descriptor>.runnableDescriptors(): List<Descriptor> {
    return filter { !it.isExpired }
}
