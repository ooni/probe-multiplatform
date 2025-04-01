package org.ooni.probe.ui.shared

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable

@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass = calculateWindowSizeClass(LocalActivity.current as Activity)
