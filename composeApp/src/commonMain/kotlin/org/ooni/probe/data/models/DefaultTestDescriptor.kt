package org.ooni.probe.data.models

import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

data class DefaultTestDescriptor(
    val label: String,
    val title: StringResource,
    val shortDescription: StringResource,
    val description: StringResource,
    val icon: DrawableResource,
    val color: Color,
    val animation: String,
    val dataUsage: StringResource,
    var netTests: List<NetTest>,
    var longRunningTests: List<NetTest> = emptyList(),
)
