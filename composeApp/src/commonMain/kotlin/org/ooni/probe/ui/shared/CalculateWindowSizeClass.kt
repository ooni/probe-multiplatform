package org.ooni.probe.ui.shared

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable

@Composable
expect fun calculateWindowSizeClass(): WindowSizeClass

@Composable
private fun heightSizeClass() = calculateWindowSizeClass().heightSizeClass

@Composable
fun isHeightCompact() = heightSizeClass() == WindowHeightSizeClass.Compact
