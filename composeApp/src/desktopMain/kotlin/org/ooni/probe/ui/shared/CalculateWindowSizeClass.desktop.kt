package org.ooni.probe.ui.shared

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable

@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass {
    return androidx.compose.material3.windowsizeclass.calculateWindowSizeClass()
}
