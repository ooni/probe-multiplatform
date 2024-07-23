package org.ooni.probe.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val PrimaryColor = Color(0xff0588cb)
val PrimaryLightColor = PrimaryColor.copy(alpha = 0.75f)

val SecondaryColor = Color(0xff5db8fe)
val SecondaryLightColor = SecondaryColor.copy(alpha = 0.75f)

// TODO: fix the color theme

val PrimaryTextColor = Color(0xffffffff)
val SecondaryTextColor = Color(0xff000000)

// val SurfaceDark = Color(0xFF1f1f1f)
// val SurfaceDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF161616)

val SurfaceLight = Color(0xFFFFFFFF)

val BackgroundLightColor = Color(0xffF1F0F5)

val BackgroundDarkColor = Color(0xff010100)

val ErrorColor = Color(0xFFFF8989)
val OnErrorColor = Color(0xFF000000)

val SuccessColor = Color(0xFF34b233)

const val SessionColor = 0xFfBA4949
const val ShortBreakColor = 0xFf38858A
const val LongBreakColor = 0xFf397097

const val Red = 0xFFFF0000
const val Orange = 0xFFFFA500
const val Blue = 0xFF0000FF
const val Green = 0xFF00FF00

const val LightGreen = 0xFF90EE90
const val Yellow = 0xFFFFFF00
const val LightBlue = 0xFFADD8E6
const val Pink = 0xFFFFC0CB

private val LightColors = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = PrimaryTextColor,
    secondary = SecondaryColor,
    onSecondary = SecondaryTextColor,
    tertiary = PrimaryLightColor,
    onTertiary = PrimaryTextColor,
    background = BackgroundLightColor,
    onBackground = Color.Black,
    surface = SurfaceLight,
    onSurface = Color.Black,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = Color.Black,
    secondaryContainer = PrimaryColor,
    onSecondaryContainer = Color.White,
    error = ErrorColor,
    onError = OnErrorColor,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryColor,
    onPrimary = PrimaryTextColor,
    secondary = SecondaryLightColor,
    onSecondary = SecondaryTextColor,
    tertiary = PrimaryLightColor,
    onTertiary = PrimaryTextColor,
    background = BackgroundDarkColor,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = Color.White,
    secondaryContainer = PrimaryColor,
    onSecondaryContainer = Color.White,
    error = ErrorColor,
    onError = OnErrorColor,
)


@Composable
internal fun Theme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) DarkColors else LightColors
    val shapes = Shapes(
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(12.dp),
    )
    val typography = MaterialTheme.typography

    MaterialTheme(
        colorScheme,
        shapes,
        typography,
        content,
    )
}