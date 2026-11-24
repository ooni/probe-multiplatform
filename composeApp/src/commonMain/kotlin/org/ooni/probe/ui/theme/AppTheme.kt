package org.ooni.probe.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.materialkolor.PaletteStyle
import com.materialkolor.material3.DynamicMaterialExpressiveTheme
import com.materialkolor.material3.rememberDynamicMaterialThemeState
import io.github.kdroidfilter.platformtools.darkmodedetector.isSystemInDarkMode
import org.ooni.probe.ui.primaryColor
import org.ooni.probe.ui.shared.LightStatusBars
import org.ooni.probe.ui.tertiaryColor

@Composable
fun AppTheme(
    useDarkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    LightStatusBars(false)

    val darkTheme = useDarkTheme ?: isSystemInDarkMode()

    val state = rememberDynamicMaterialThemeState(
        seedColor = primaryColor,
        isDark = darkTheme,
        style = PaletteStyle.Cmf(tertiarySeedColor = tertiaryColor),
    )

    CompositionLocalProvider(
        LocalCustomColors provides if (darkTheme) customColorsDark else customColorsLight,
    ) {
        DynamicMaterialExpressiveTheme(
            state = state,
            typography = AppTypography(),
            content = content,
        )
    }
}
