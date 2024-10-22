package org.ooni.probe.ui.shared

import android.app.Activity
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass = calculateWindowSizeClass(LocalContext.current as Activity)
