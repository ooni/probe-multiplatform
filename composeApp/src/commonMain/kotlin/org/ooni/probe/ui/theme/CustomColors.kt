package org.ooni.probe.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// App specific colors outside of the Material Theme Colors

val successColorLight = Color(0xFF40c057)
val onSuccessColorLight = Color.White
val successColorDark = Color(0xFF2b8a3e)
val onSuccessColorDark = Color.White

data class CustomColors(
    val success: Color,
    val onSuccess: Color,
)

val customColorsLight = CustomColors(successColorLight, onSuccessColorLight)
val customColorsDark = CustomColors(successColorDark, onSuccessColorDark)

internal val LocalCustomColors = staticCompositionLocalOf { customColorsLight }

val MaterialTheme.customColors: CustomColors
    @Composable
    @ReadOnlyComposable
    get() = LocalCustomColors.current
