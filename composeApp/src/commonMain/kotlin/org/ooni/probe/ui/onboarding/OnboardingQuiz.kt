package org.ooni.probe.ui.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import ooniprobe.composeapp.generated.resources.Onboarding_PopQuiz_1_Question
import ooniprobe.composeapp.generated.resources.Onboarding_PopQuiz_1_Title
import ooniprobe.composeapp.generated.resources.Onboarding_PopQuiz_1_Wrong_Paragraph
import ooniprobe.composeapp.generated.resources.Onboarding_PopQuiz_1_Wrong_Title
import ooniprobe.composeapp.generated.resources.Onboarding_PopQuiz_2_Question
import ooniprobe.composeapp.generated.resources.Onboarding_PopQuiz_2_Title
import ooniprobe.composeapp.generated.resources.Onboarding_PopQuiz_2_Wrong_Paragraph
import ooniprobe.composeapp.generated.resources.Onboarding_PopQuiz_2_Wrong_Title
import ooniprobe.composeapp.generated.resources.Onboarding_PopQuiz_False
import ooniprobe.composeapp.generated.resources.Onboarding_PopQuiz_Title
import ooniprobe.composeapp.generated.resources.Onboarding_PopQuiz_True
import ooniprobe.composeapp.generated.resources.Onboarding_PopQuiz_Wrong_Button_Back
import ooniprobe.composeapp.generated.resources.Onboarding_PopQuiz_Wrong_Button_Continue
import ooniprobe.composeapp.generated.resources.Onboarding_QuizAnswer_Correct
import ooniprobe.composeapp.generated.resources.Onboarding_QuizAnswer_Incorrect
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.Animation
import org.ooni.probe.ui.shared.LottieAnimation
import org.ooni.probe.ui.theme.LocalCustomColors

@Composable
fun OnboardingQuiz(
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    var questionIndex by remember { mutableStateOf(0) }
    val question = QUESTIONS[questionIndex]
    var type by remember { mutableStateOf(DialogType.Quiz) }

    fun nextQuestion() {
        if (questionIndex == QUESTIONS.size - 1) {
            onFinish()
        } else {
            type = DialogType.Quiz
            questionIndex++
        }
    }

    BasicAlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        when (type) {
            DialogType.Quiz ->
                Quiz(
                    question,
                    onTrue = { type = DialogType.CorrectAnimation },
                    onFalse = { type = DialogType.IncorrectAnimation },
                )

            DialogType.CorrectAnimation ->
                ResultAnimation(
                    isCorrect = true,
                    onFinish = { nextQuestion() },
                )

            DialogType.IncorrectAnimation ->
                ResultAnimation(
                    isCorrect = false,
                    onFinish = { type = DialogType.Warning },
                )

            DialogType.Warning ->
                Warning(
                    question,
                    onBack = { onBack() },
                    onContinue = { nextQuestion() },
                )
        }
    }
}

@Composable
private fun Quiz(
    question: Question,
    onTrue: () -> Unit,
    onFalse: () -> Unit,
) {
    Surface(
        color = LocalCustomColors.current.quiz,
        contentColor = LocalCustomColors.current.onQuiz,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.Onboarding_PopQuiz_Title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(8.dp),
            )
            HorizontalDivider(color = LocalContentColor.current)
            Text(
                text = stringResource(question.title),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(question.question),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
                    .padding(bottom = 16.dp),
            )

            Row(Modifier.fillMaxWidth()) {
                Button(
                    onClick = onTrue,
                    shape = RoundedCornerShape(0.dp),
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalCustomColors.current.quizTrue,
                        contentColor = LocalCustomColors.current.onQuizTrue,
                    ),
                    modifier = Modifier
                        .height(52.dp)
                        .weight(1f)
                        .testTag("Quiz-True"),
                ) {
                    Text(
                        stringResource(Res.string.Onboarding_PopQuiz_True),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Button(
                    onClick = onFalse,
                    shape = RoundedCornerShape(0.dp),
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalCustomColors.current.quizFalse,
                        contentColor = LocalCustomColors.current.onQuizFalse,
                    ),
                    modifier = Modifier
                        .height(52.dp)
                        .weight(1f),
                ) {
                    Text(
                        stringResource(Res.string.Onboarding_PopQuiz_False),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultAnimation(
    isCorrect: Boolean,
    onFinish: () -> Unit,
) {
    Surface(
        color = if (isCorrect) {
            LocalCustomColors.current.quizTrueAnimation
        } else {
            LocalCustomColors.current.quizFalseAnimation
        },
        contentColor = LocalCustomColors.current.onQuiz,
        shape = RoundedCornerShape(24.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
        ) {
            LottieAnimation(
                animation = if (isCorrect) Animation.QuizCorrect else Animation.QuizIncorrect,
                contentDescription = stringResource(
                    if (isCorrect) {
                        Res.string.Onboarding_QuizAnswer_Correct
                    } else {
                        Res.string.Onboarding_QuizAnswer_Incorrect
                    },
                ),
                restartOnPlay = false,
                onFinish = onFinish,
                modifier = Modifier.size(200.dp),
            )
        }
    }
}

@Composable
private fun Warning(
    question: Question,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    Surface(
        color = LocalCustomColors.current.quizWarning,
        contentColor = LocalCustomColors.current.onQuizWarning,
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(question.wrongTitle),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(8.dp),
            )
            HorizontalDivider(color = LocalContentColor.current)
            Text(
                text = stringResource(question.wrongText),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
                    .padding(bottom = 16.dp),
            )

            Row(Modifier.fillMaxWidth()) {
                Button(
                    onClick = onBack,
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalCustomColors.current.quizWarningBack,
                        contentColor = LocalCustomColors.current.onQuizWarningBack,
                    ),
                    modifier = Modifier
                        .height(52.dp)
                        .weight(1f),
                ) {
                    Text(
                        stringResource(Res.string.Onboarding_PopQuiz_Wrong_Button_Back),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Button(
                    onClick = onContinue,
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalCustomColors.current.onQuizWarning,
                        contentColor = LocalCustomColors.current.quizWarning,
                    ),
                    modifier = Modifier
                        .height(52.dp)
                        .weight(1f),
                ) {
                    Text(
                        stringResource(Res.string.Onboarding_PopQuiz_Wrong_Button_Continue),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

private enum class DialogType {
    Quiz,
    Warning,
    CorrectAnimation,
    IncorrectAnimation,
}

private val QUESTIONS = listOf(
    Question(
        title = Res.string.Onboarding_PopQuiz_1_Title,
        question = Res.string.Onboarding_PopQuiz_1_Question,
        wrongTitle = Res.string.Onboarding_PopQuiz_1_Wrong_Title,
        wrongText = Res.string.Onboarding_PopQuiz_1_Wrong_Paragraph,
    ),
    Question(
        title = Res.string.Onboarding_PopQuiz_2_Title,
        question = Res.string.Onboarding_PopQuiz_2_Question,
        wrongTitle = Res.string.Onboarding_PopQuiz_2_Wrong_Title,
        wrongText = Res.string.Onboarding_PopQuiz_2_Wrong_Paragraph,
    ),
)

private data class Question(
    val title: StringResource,
    val question: StringResource,
    val wrongTitle: StringResource,
    val wrongText: StringResource,
)
