package org.ooni.probe.ui.shared

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieAnimationState
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import ooniprobe.composeapp.generated.resources.Res
import org.ooni.probe.data.models.Animation

@Composable
fun LottieAnimation(
    animation: Animation,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    restartOnPlay: Boolean = true,
    onFinish: (() -> Unit)? = null,
) {
    val composition by rememberLottieComposition(animation) {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/anim/${animation.fileName}.json").decodeToString(),
        )
    }
    val progress: LottieAnimationState = animateLottieCompositionAsState(
        composition = composition,
        restartOnPlay = restartOnPlay,
        iterations = if (restartOnPlay) Compottie.IterateForever else 1,
    )

    Image(
        painter = rememberLottiePainter(
            composition = composition,
            progress = { progress.value },
        ),
        contentDescription = contentDescription,
        modifier = modifier,
    )

    if (progress.isPlaying && progress.isAtEnd) {
        onFinish?.invoke()
    }
}
