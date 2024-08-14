package org.ooni.probe.data.models

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.DrawableResource

data class Descriptor(
    val name: String,
    val title: @Composable () -> String,
    val shortDescription: @Composable () -> String?,
    val description: @Composable () -> String?,
    val icon: DrawableResource?,
    val color: Color?,
    val animation: String?,
    val dataUsage: @Composable () -> String?,
    val netTests: List<NetTest>,
    val longRunningTests: List<NetTest>? = null,
    val source: Source,
) {
    sealed interface Source {
        data class Default(val value: DefaultTestDescriptor) : Source

        data class Installed(val value: InstalledTestDescriptorModel) : Source
    }
}
