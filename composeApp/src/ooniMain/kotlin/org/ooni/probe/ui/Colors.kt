package org.ooni.probe.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.ooni.probe.shared.ThemeConfigBase

class ThemeConfig {
    companion object : ThemeConfigBase {
        override fun colorScheme(isDarkTheme: Boolean) = if (isDarkTheme) darkColors else lightColors
    }
}

val primaryColor = Color(0xff0588cb)
val primaryLightColor = primaryColor.copy(alpha = 0.75f)
val secondaryColor = Color(0xff5db8fe)
val secondaryLightColor = secondaryColor.copy(alpha = 0.75f)
val primaryTextColor = Color(0xffffffff)
val secondaryTextColor = Color(0xff000000)
val surfaceDark = Color(0xFF161616)
val surfaceLight = Color(0xFFFFFFFF)
val backgroundLightColor = Color(0xffF1F0F5)
val backgroundDarkColor = Color(0xff010100)
val errorColor = Color(0xFFFF8989)
val onErrorColor = Color(0xFF000000)

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
