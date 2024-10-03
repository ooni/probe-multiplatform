package org.ooni.probe.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import org.ooni.probe.ui.navigation.Screen
import org.ooni.probe.ui.shared.LightStatusBars

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    currentRoute: String? = null,
    content: @Composable () -> Unit,
) {
    LightStatusBars(
        !useDarkTheme &&
            currentRoute != Screen.Onboarding.route &&
            currentRoute != Screen.Dashboard.route &&
            currentRoute != Screen.RunningTest.route,
    )

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
