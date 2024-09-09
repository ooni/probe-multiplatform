package org.ooni.probe.ui.shared

import androidx.compose.ui.state.ToggleableState

data class SelectableItem<I>(
    val item: I,
    val isSelected: Boolean,
)

data class ParentSelectableItem<I>(
    val item: I,
    val state: ToggleableState,
    val isExpanded: Boolean,
)
