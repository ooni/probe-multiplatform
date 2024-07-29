package org.ooni.probe.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

class ThemeConfig {
    companion object {
        fun colorScheme(isDarkTheme: Boolean) = if (isDarkTheme) darkColors else lightColors
    }
}

private val primaryColor = Color(0xffD32625)
private val primaryLightColor = primaryColor.copy(alpha = 0.75f)
private val secondaryColor = Color(0xff5db8fe)
private val secondaryLightColor = secondaryColor.copy(alpha = 0.75f)
private val primaryTextColor = Color(0xffffffff)
private val secondaryTextColor = Color(0xff000000)
private val surfaceDark = Color(0xFF161616)
private val surfaceLight = Color(0xFFFFFFFF)
private val backgroundLightColor = Color(0xffF1F0F5)
private val backgroundDarkColor = Color(0xff010100)
private val errorColor = Color(0xFFFF8989)
private val onErrorColor = Color(0xFF000000)

val darkColors: ColorScheme =
    darkColorScheme(
        primary = primaryColor,
        onPrimary = primaryTextColor,
        secondary = secondaryLightColor,
        onSecondary = secondaryTextColor,
        tertiary = primaryLightColor,
        onTertiary = primaryTextColor,
        background = backgroundDarkColor,
        onBackground = Color.White,
        surface = surfaceDark,
        onSurface = Color.White,
        surfaceVariant = surfaceDark,
        onSurfaceVariant = Color.White,
        secondaryContainer = primaryColor,
        onSecondaryContainer = Color.White,
        error = errorColor,
        onError = onErrorColor,
    )
val lightColors: ColorScheme =
    lightColorScheme(
        primary = primaryColor,
        onPrimary = primaryTextColor,
        secondary = secondaryColor,
        onSecondary = secondaryTextColor,
        tertiary = primaryLightColor,
        onTertiary = primaryTextColor,
        background = backgroundLightColor,
        onBackground = Color.Black,
        surface = surfaceLight,
        onSurface = Color.Black,
        surfaceVariant = surfaceLight,
        onSurfaceVariant = Color.Black,
        secondaryContainer = primaryColor,
        onSecondaryContainer = Color.White,
        error = errorColor,
        onError = onErrorColor,
    )
