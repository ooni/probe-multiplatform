package org.ooni.probe.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import org.ooni.probe.ui.backgroundDark
import org.ooni.probe.ui.backgroundLight
import org.ooni.probe.ui.errorContainerDark
import org.ooni.probe.ui.errorContainerLight
import org.ooni.probe.ui.errorDark
import org.ooni.probe.ui.errorLight
import org.ooni.probe.ui.inverseOnSurfaceDark
import org.ooni.probe.ui.inverseOnSurfaceLight
import org.ooni.probe.ui.inversePrimaryDark
import org.ooni.probe.ui.inversePrimaryLight
import org.ooni.probe.ui.inverseSurfaceDark
import org.ooni.probe.ui.inverseSurfaceLight
import org.ooni.probe.ui.onBackgroundDark
import org.ooni.probe.ui.onBackgroundLight
import org.ooni.probe.ui.onErrorContainerDark
import org.ooni.probe.ui.onErrorContainerLight
import org.ooni.probe.ui.onErrorDark
import org.ooni.probe.ui.onErrorLight
import org.ooni.probe.ui.onPrimaryContainerDark
import org.ooni.probe.ui.onPrimaryContainerLight
import org.ooni.probe.ui.onPrimaryDark
import org.ooni.probe.ui.onPrimaryLight
import org.ooni.probe.ui.onSecondaryContainerDark
import org.ooni.probe.ui.onSecondaryContainerLight
import org.ooni.probe.ui.onSecondaryDark
import org.ooni.probe.ui.onSecondaryLight
import org.ooni.probe.ui.onSurfaceDark
import org.ooni.probe.ui.onSurfaceLight
import org.ooni.probe.ui.onSurfaceVariantDark
import org.ooni.probe.ui.onSurfaceVariantLight
import org.ooni.probe.ui.onTertiaryContainerDark
import org.ooni.probe.ui.onTertiaryContainerLight
import org.ooni.probe.ui.onTertiaryDark
import org.ooni.probe.ui.onTertiaryLight
import org.ooni.probe.ui.outlineDark
import org.ooni.probe.ui.outlineLight
import org.ooni.probe.ui.outlineVariantDark
import org.ooni.probe.ui.outlineVariantLight
import org.ooni.probe.ui.primaryContainerDark
import org.ooni.probe.ui.primaryContainerLight
import org.ooni.probe.ui.primaryDark
import org.ooni.probe.ui.primaryLight
import org.ooni.probe.ui.scrimDark
import org.ooni.probe.ui.scrimLight
import org.ooni.probe.ui.secondaryContainerDark
import org.ooni.probe.ui.secondaryContainerLight
import org.ooni.probe.ui.secondaryDark
import org.ooni.probe.ui.secondaryLight
import org.ooni.probe.ui.surfaceBrightDark
import org.ooni.probe.ui.surfaceBrightLight
import org.ooni.probe.ui.surfaceContainerDark
import org.ooni.probe.ui.surfaceContainerHighDark
import org.ooni.probe.ui.surfaceContainerHighLight
import org.ooni.probe.ui.surfaceContainerHighestDark
import org.ooni.probe.ui.surfaceContainerHighestLight
import org.ooni.probe.ui.surfaceContainerLight
import org.ooni.probe.ui.surfaceContainerLowDark
import org.ooni.probe.ui.surfaceContainerLowLight
import org.ooni.probe.ui.surfaceContainerLowestDark
import org.ooni.probe.ui.surfaceContainerLowestLight
import org.ooni.probe.ui.surfaceDark
import org.ooni.probe.ui.surfaceDimDark
import org.ooni.probe.ui.surfaceDimLight
import org.ooni.probe.ui.surfaceLight
import org.ooni.probe.ui.surfaceVariantDark
import org.ooni.probe.ui.surfaceVariantLight
import org.ooni.probe.ui.tertiaryContainerDark
import org.ooni.probe.ui.tertiaryContainerLight
import org.ooni.probe.ui.tertiaryDark
import org.ooni.probe.ui.tertiaryLight

val lightScheme =
    lightColorScheme(
        primary = primaryLight,
        onPrimary = onPrimaryLight,
        primaryContainer = primaryContainerLight,
        onPrimaryContainer = onPrimaryContainerLight,
        secondary = secondaryLight,
        onSecondary = onSecondaryLight,
        secondaryContainer = secondaryContainerLight,
        onSecondaryContainer = onSecondaryContainerLight,
        tertiary = tertiaryLight,
        onTertiary = onTertiaryLight,
        tertiaryContainer = tertiaryContainerLight,
        onTertiaryContainer = onTertiaryContainerLight,
        error = errorLight,
        onError = onErrorLight,
        errorContainer = errorContainerLight,
        onErrorContainer = onErrorContainerLight,
        background = backgroundLight,
        onBackground = onBackgroundLight,
        surface = surfaceLight,
        onSurface = onSurfaceLight,
        surfaceVariant = surfaceVariantLight,
        onSurfaceVariant = onSurfaceVariantLight,
        outline = outlineLight,
        outlineVariant = outlineVariantLight,
        scrim = scrimLight,
        inverseSurface = inverseSurfaceLight,
        inverseOnSurface = inverseOnSurfaceLight,
        inversePrimary = inversePrimaryLight,
        surfaceDim = surfaceDimLight,
        surfaceBright = surfaceBrightLight,
        surfaceContainerLowest = surfaceContainerLowestLight,
        surfaceContainerLow = surfaceContainerLowLight,
        surfaceContainer = surfaceContainerLight,
        surfaceContainerHigh = surfaceContainerHighLight,
        surfaceContainerHighest = surfaceContainerHighestLight,
    )

val darkScheme =
    darkColorScheme(
        primary = primaryDark,
        onPrimary = onPrimaryDark,
        primaryContainer = primaryContainerDark,
        onPrimaryContainer = onPrimaryContainerDark,
        secondary = secondaryDark,
        onSecondary = onSecondaryDark,
        secondaryContainer = secondaryContainerDark,
        onSecondaryContainer = onSecondaryContainerDark,
        tertiary = tertiaryDark,
        onTertiary = onTertiaryDark,
        tertiaryContainer = tertiaryContainerDark,
        onTertiaryContainer = onTertiaryContainerDark,
        error = errorDark,
        onError = onErrorDark,
        errorContainer = errorContainerDark,
        onErrorContainer = onErrorContainerDark,
        background = backgroundDark,
        onBackground = onBackgroundDark,
        surface = surfaceDark,
        onSurface = onSurfaceDark,
        surfaceVariant = surfaceVariantDark,
        onSurfaceVariant = onSurfaceVariantDark,
        outline = outlineDark,
        outlineVariant = outlineVariantDark,
        scrim = scrimDark,
        inverseSurface = inverseSurfaceDark,
        inverseOnSurface = inverseOnSurfaceDark,
        inversePrimary = inversePrimaryDark,
        surfaceDim = surfaceDimDark,
        surfaceBright = surfaceBrightDark,
        surfaceContainerLowest = surfaceContainerLowestDark,
        surfaceContainerLow = surfaceContainerLowDark,
        surfaceContainer = surfaceContainerDark,
        surfaceContainerHigh = surfaceContainerHighDark,
        surfaceContainerHighest = surfaceContainerHighestDark,
    )
