package org.ooni.probe.ui.shared

import KottieAnimation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kottieComposition.KottieCompositionSpec
import kottieComposition.animateKottieCompositionAsState
import kottieComposition.rememberKottieComposition
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.MissingResourceException

@Composable
fun LottieAnimation(
    fileName: String,
    contentDescription: String?,
    // Transparent background color isn't working https://github.com/ismai117/kottie/issues/15
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    restartOnPlay: Boolean = true,
    onFinish: (() -> Unit)? = null,
) {
    var animation by remember { mutableStateOf("") }
    LaunchedEffect(fileName) {
        withContext(Dispatchers.IO) {
            try {
                animation = Res.readBytes("files/anim/$fileName.json").decodeToString()
            } catch (e: MissingResourceException) {
                Logger.e("Failed to load animation: $fileName", e)
            }
        }
    }
    val animComposition = rememberKottieComposition(spec = KottieCompositionSpec.File(animation))
    val animState by animateKottieCompositionAsState(
        composition = animComposition,
        restartOnPlay = restartOnPlay,
    )

    if (animState.isCompleted) {
        onFinish?.invoke()
    }

    KottieAnimation(
        composition = animComposition,
        progress = { animState.progress },
        backgroundColor = backgroundColor,
        modifier = modifier
            .run {
                contentDescription
                    ?.let { semantics { this.contentDescription = contentDescription } }
                    ?: this
            },
    )
}
