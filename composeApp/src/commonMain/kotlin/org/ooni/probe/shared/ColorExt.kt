package org.ooni.probe.shared

import androidx.compose.ui.graphics.Color

fun String.hexToColor(): Color {
    val baseNumber = filter { it != '#' }
    val fullNumber =
        if (baseNumber.length == 6) {
            "FF$baseNumber"
        } else {
            baseNumber
        }
    return Color(fullNumber.hexToInt())
}
