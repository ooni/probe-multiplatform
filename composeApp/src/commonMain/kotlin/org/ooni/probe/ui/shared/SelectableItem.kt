package org.ooni.probe.ui.shared

data class SelectableItem<I>(
    val item: I,
    val isSelected: Boolean,
)

data class SelectableAndCollapsableItem<I>(
    val item: I,
    val isSelected: Boolean,
    val isExpanded: Boolean,
)
