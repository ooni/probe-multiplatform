package org.ooni.probe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import io.github.kdroidfilter.platformtools.darkmodedetector.isSystemInDarkMode
import org.ooni.probe.ui.shared.LightStatusBars

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkMode(),
    content: @Composable () -> Unit,
) {
    LightStatusBars(false)

    CompositionLocalProvider(
        LocalCustomColors provides if (useDarkTheme) customColorsDark else customColorsLight,
    ) {
        MaterialTheme(
            colorScheme = if (useDarkTheme) darkScheme else lightScheme,
            typography = AppTypography(),
            content = content,
        )
    }
}
