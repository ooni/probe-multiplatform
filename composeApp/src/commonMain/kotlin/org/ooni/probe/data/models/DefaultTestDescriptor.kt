package org.ooni.probe.data.models

import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.ooni.engine.models.SummaryType

data class DefaultTestDescriptor(
    val label: String,
    val title: StringResource,
    val shortDescription: StringResource,
    val description: StringResource,
    val icon: DrawableResource,
    val color: Color,
    val animation: Animation,
    val dataUsage: StringResource,
    val netTests: List<NetTest>,
    val longRunningTests: List<NetTest> = emptyList(),
    val summaryType: SummaryType,
)
