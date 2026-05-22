package org.ooni.probe.screenshots

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.app_name
import ooniprobe.composeapp.generated.resources.ooni_colored_logo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Wraps [content] in a Windows 11-style window (light title bar with the app icon and title
 * on the left, minimize / maximize / close caption buttons on the right, lightly rounded
 * corners, drop shadow) over a desktop background. The Windows counterpart to
 * [MacScreenshotFrame]: it dresses Microsoft Store screenshots so they read as the OONI Probe
 * Desktop window sitting on a real Windows 11 machine.
 */
@Composable
fun WindowsScreenshotFrame(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DESKTOP_BG),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .padding(top = WINDOW_TOP_MARGIN_DP, bottom = WINDOW_BOTTOM_MARGIN_DP)
                    .fillMaxHeight()
                    .aspectRatio(APP_WINDOW_RATIO, matchHeightConstraintsFirst = true)
                    .shadow(elevation = 24.dp, shape = RoundedCornerShape(WINDOW_CORNER_RADIUS_DP))
                    .clip(RoundedCornerShape(WINDOW_CORNER_RADIUS_DP)),
                color = Color.White,
            ) {
                Column(Modifier.fillMaxSize()) {
                    WindowsTitleBar()
                    Box(Modifier.fillMaxSize()) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun WindowsTitleBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TITLE_BAR_HEIGHT_DP)
            .background(TITLE_BAR_BG),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: app icon + window title, the way Windows 11 renders the system title bar.
        Image(
            painter = painterResource(Res.drawable.ooni_colored_logo),
            contentDescription = null,
            modifier = Modifier
                .padding(start = 10.dp, end = 8.dp)
                .size(16.dp),
        )
        Text(
            text = stringResource(Res.string.app_name),
            color = TITLE_TEXT_COLOR,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = true),
        )
        // Right: minimize / maximize / close caption buttons (Win11 square hit-areas).
        Row(horizontalArrangement = Arrangement.End) {
            CaptionButton(Glyph.Minimize)
            CaptionButton(Glyph.Maximize)
            CaptionButton(Glyph.Close)
        }
    }
}

private enum class Glyph { Minimize, Maximize, Close }

@Composable
private fun CaptionButton(glyph: Glyph) {
    Box(
        modifier = Modifier
            .width(CAPTION_BUTTON_WIDTH_DP)
            .height(TITLE_BAR_HEIGHT_DP),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(GLYPH_SIZE_DP)) {
            val stroke = Stroke(width = GLYPH_STROKE_PX)
            val w = size.width
            val h = size.height
            when (glyph) {
                Glyph.Minimize ->
                    drawLine(
                        color = GLYPH_COLOR,
                        start = Offset(0f, h / 2f),
                        end = Offset(w, h / 2f),
                        strokeWidth = GLYPH_STROKE_PX,
                    )

                Glyph.Maximize ->
                    drawRect(
                        color = GLYPH_COLOR,
                        topLeft = Offset(0f, 0f),
                        size = Size(w, h),
                        style = stroke,
                    )

                Glyph.Close -> {
                    drawLine(
                        color = GLYPH_COLOR,
                        start = Offset(0f, 0f),
                        end = Offset(w, h),
                        strokeWidth = GLYPH_STROKE_PX,
                    )
                    drawLine(
                        color = GLYPH_COLOR,
                        start = Offset(w, 0f),
                        end = Offset(0f, h),
                        strokeWidth = GLYPH_STROKE_PX,
                    )
                }
            }
        }
    }
}

/**
 * Width-to-height ratio of the OONI Probe desktop window
 * (`Main.kt` → `DpSize(480.dp, 800.dp)`). The screenshot window is sized to fill the
 * available height and derives its width from this ratio so the captured chrome stays
 * portrait 3:5 regardless of which Microsoft Store viewport is selected.
 */
private const val APP_WINDOW_RATIO: Float = 480f / 800f

private val WINDOW_TOP_MARGIN_DP: Dp = 8.dp
private val WINDOW_BOTTOM_MARGIN_DP: Dp = 16.dp

// Windows 11 windows use subtle 8dp corner rounding.
private val WINDOW_CORNER_RADIUS_DP: Dp = 8.dp

// Win11 title bar is 32px tall; caption buttons are 46x32.
private val TITLE_BAR_HEIGHT_DP: Dp = 32.dp
private val CAPTION_BUTTON_WIDTH_DP: Dp = 44.dp
private val GLYPH_SIZE_DP: Dp = 10.dp
private const val GLYPH_STROKE_PX: Float = 1.4f

// Light "Mica"-like title bar fill with dark glyphs/text.
private val TITLE_BAR_BG = Color(0xFFF3F3F3)
private val TITLE_TEXT_COLOR = Color(0xFF202020)
private val GLYPH_COLOR = Color(0xFF202020)
private val DESKTOP_BG = Color(0xff0588cb)
