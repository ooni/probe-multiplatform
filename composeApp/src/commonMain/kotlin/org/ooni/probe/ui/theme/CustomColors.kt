package org.ooni.probe.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.ooni.probe.ui.onOnboardingColor
import org.ooni.probe.ui.onQuizColor
import org.ooni.probe.ui.onQuizFalseColor
import org.ooni.probe.ui.onQuizTrueColor
import org.ooni.probe.ui.onboarding1Color
import org.ooni.probe.ui.onboarding2Color
import org.ooni.probe.ui.onboarding3Color
import org.ooni.probe.ui.quizColor
import org.ooni.probe.ui.quizFalseAnimationColor
import org.ooni.probe.ui.quizFalseColor
import org.ooni.probe.ui.quizTrueAnimationColor
import org.ooni.probe.ui.quizTrueColor

// App specific colors outside of the Material Theme Colors

val successColorLight = Color(0xFF40c057)
val onSuccessColorLight = Color.White
val successColorDark = Color(0xFF2b8a3e)
val onSuccessColorDark = Color.White
val onDescriptorColorLight = Color.White
val onDescriptorColorDark = Color.White

val quizWarningColor = Color(0xFF495057)
val onQuizWarningColor = Color.White
val quizWarningBackColor = Color(0xFF343a40)
val onQuizWarningBackColor = Color.White

val logDebugColor = Color.Unspecified
val logInfoColor = Color(0xFF2b8a3e)
val logWarnColor = Color(0xFFd9480f)
val logErrorColor = Color(0xFFc92a2a)

data class CustomColors(
    val success: Color,
    val onSuccess: Color,
    val onDescriptor: Color,
    val onboarding1: Color = onboarding1Color,
    val onboarding2: Color = onboarding2Color,
    val onboarding3: Color = onboarding3Color,
    val onOnboarding: Color = onOnboardingColor,
    val quiz: Color = quizColor,
    val onQuiz: Color = onQuizColor,
    val quizTrue: Color = quizTrueColor,
    val onQuizTrue: Color = onQuizTrueColor,
    val quizTrueAnimation: Color = quizTrueAnimationColor,
    val quizFalse: Color = quizFalseColor,
    val onQuizFalse: Color = onQuizFalseColor,
    val quizFalseAnimation: Color = quizFalseAnimationColor,
    val quizWarning: Color = quizWarningColor,
    val onQuizWarning: Color = onQuizWarningColor,
    val quizWarningBack: Color = quizWarningBackColor,
    val onQuizWarningBack: Color = onQuizWarningBackColor,
    val logDebug: Color = logDebugColor,
    val logInfo: Color = logInfoColor,
    val logWarn: Color = logWarnColor,
    val logError: Color = logErrorColor,
)

val customColorsLight = CustomColors(
    success = successColorLight,
    onSuccess = onSuccessColorLight,
    onDescriptor = onDescriptorColorLight,
)
val customColorsDark = CustomColors(
    success = successColorDark,
    onSuccess = onSuccessColorDark,
    onDescriptor = onDescriptorColorDark,
)

internal val LocalCustomColors = staticCompositionLocalOf { customColorsLight }

val MaterialTheme.customColors: CustomColors
    @Composable
    @ReadOnlyComposable
    get() = LocalCustomColors.current
