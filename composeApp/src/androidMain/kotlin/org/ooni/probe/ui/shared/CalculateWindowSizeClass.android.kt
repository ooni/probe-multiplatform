package org.ooni.probe.ui.shared

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass =
    LocalActivity.current
        ?.let { calculateWindowSizeClass(it) }
        // Generic default size
        ?: WindowSizeClass.calculateFromSize(DpSize(360.dp, 800.dp))
