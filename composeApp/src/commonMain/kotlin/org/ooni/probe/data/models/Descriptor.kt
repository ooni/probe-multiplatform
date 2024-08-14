package org.ooni.probe.data.models

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.DrawableResource
import org.ooni.probe.shared.now

data class Descriptor(
    val name: String,
    val title: @Composable () -> String,
    val shortDescription: @Composable () -> String?,
    val description: @Composable () -> String?,
    val icon: DrawableResource?,
    val color: Color?,
    val animation: String?,
    val dataUsage: @Composable () -> String?,
    val expirationDate: LocalDateTime?,
    val netTests: List<NetTest>,
    val longRunningTests: List<NetTest> = emptyList(),
    val source: Source,
) {
    sealed interface Source {
        data class Default(val value: DefaultTestDescriptor) : Source

        data class Installed(val value: InstalledTestDescriptorModel) : Source
    }

    val isExpired get() = expirationDate != null && expirationDate < LocalDateTime.now()
}
