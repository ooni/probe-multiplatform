package org.ooni.probe.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.firasans_italic
import ooniprobe.composeapp.generated.resources.firasans_regular
import ooniprobe.composeapp.generated.resources.firasans_semibold
import org.jetbrains.compose.resources.Font

@Composable
fun customFontFamily() =
    FontFamily(
        Font(
            resource = Res.font.firasans_regular,
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
        ),
        Font(
            resource = Res.font.firasans_italic,
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
        ),
        Font(
            resource = Res.font.firasans_semibold,
            weight = FontWeight.Bold,
            style = FontStyle.Normal,
        ),
    )

private val defaultTypography = Typography()

@Composable
fun AppTypography() =
    Typography(
        displayLarge = defaultTypography.displayLarge.copy(fontFamily = customFontFamily()),
        displayMedium = defaultTypography.displayMedium.copy(fontFamily = customFontFamily()),
        displaySmall = defaultTypography.displaySmall.copy(fontFamily = customFontFamily()),
        headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = customFontFamily()),
        headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = customFontFamily()),
        headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = customFontFamily()),
        titleLarge = defaultTypography.titleLarge.copy(fontFamily = customFontFamily()),
        titleMedium = defaultTypography.titleMedium.copy(
            fontFamily = customFontFamily(),
            fontSize = 18.sp,
        ),
        titleSmall = defaultTypography.titleSmall.copy(fontFamily = customFontFamily()),
        bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = customFontFamily()),
        bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = customFontFamily()),
        bodySmall = defaultTypography.bodySmall.copy(fontFamily = customFontFamily()),
        labelLarge = defaultTypography.labelLarge.copy(fontFamily = customFontFamily()),
        labelMedium = defaultTypography.labelMedium.copy(fontFamily = customFontFamily()),
        labelSmall = defaultTypography.labelSmall.copy(fontFamily = customFontFamily()),
    )
