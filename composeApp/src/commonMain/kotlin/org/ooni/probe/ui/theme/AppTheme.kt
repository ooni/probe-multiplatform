package org.ooni.probe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import io.github.kdroidfilter.platformtools.darkmodedetector.isSystemInDarkMode
import org.ooni.probe.ui.shared.LightStatusBars

@Composable
fun AppTheme(
    useDarkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    LightStatusBars(false)

    val darkTheme = useDarkTheme ?: isSystemInDarkMode()

    CompositionLocalProvider(
        LocalCustomColors provides if (darkTheme) customColorsDark else customColorsLight,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkScheme else lightScheme,
            typography = AppTypography(),
            content = content,
        )
    }
}
