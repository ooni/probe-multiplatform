package org.ooni.probe.ui.shared

import androidx.compose.ui.Modifier

fun Modifier.applyIf(
    condition: Boolean,
    mapper: Modifier.() -> Modifier,
) = if (condition) {
    mapper()
} else {
    this
}
