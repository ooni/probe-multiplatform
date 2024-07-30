package org.ooni.probe.shared

import androidx.compose.material3.ColorScheme

interface ThemeConfigBase {
    fun colorScheme(isDarkTheme: Boolean): ColorScheme
}
