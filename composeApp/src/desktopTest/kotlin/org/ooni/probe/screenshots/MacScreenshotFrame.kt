package org.ooni.probe.screenshots

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Wraps [content] in a macOS-style window (traffic-light title bar, rounded corners,
 * drop shadow) under an empty macOS system menu bar, over a white desktop background.
 * Used to dress App Store screenshots so they look like the OONI Probe Desktop window
 * sitting on a real Mac.
 */
@Composable
fun MacScreenshotFrame(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DESKTOP_BG),
    ) {
        MacSystemMenuBar()
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
                    MacTitleBar()
                    Box(Modifier.fillMaxSize()) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
private fun MacSystemMenuBar() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MENU_BAR_HEIGHT_DP)
                .background(MENU_BAR_BG),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MENU_BAR_HAIRLINE_DP)
                .background(MENU_BAR_BORDER),
        )
    }
}

@Composable
private fun MacTitleBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TITLE_BAR_HEIGHT_DP)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFECECEC), Color(0xFFD6D6D6)),
                ),
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TrafficLightDot(Color(0xFFFF5F57))
            TrafficLightDot(Color(0xFFFEBC2E))
            TrafficLightDot(Color(0xFF28C840))
        }
    }
}

@Composable
private fun TrafficLightDot(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color = color, shape = CircleShape),
    )
}

/**
 * Width-to-height ratio of the OONI Probe desktop window
 * (`Main.kt` → `DpSize(480.dp, 800.dp)`). The screenshot window is sized to fill the
 * available height and derives its width from this ratio so the captured chrome stays
 * portrait 3:5 regardless of which Mac App Store viewport is selected.
 */
private const val APP_WINDOW_RATIO: Float = 480f / 800f

private val WINDOW_TOP_MARGIN_DP: Dp = 8.dp
private val WINDOW_BOTTOM_MARGIN_DP: Dp = 16.dp
private val WINDOW_CORNER_RADIUS_DP: Dp = 10.dp
private val TITLE_BAR_HEIGHT_DP: Dp = 28.dp
private val MENU_BAR_HEIGHT_DP: Dp = 24.dp
private val MENU_BAR_HAIRLINE_DP: Dp = 1.dp
private val DESKTOP_BG = Color.White
private val MENU_BAR_BG = Color(0xFFF6F6F6)
private val MENU_BAR_BORDER = Color(0x14000000)
