package org.ooni.probe.ui.shared

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

@Composable
fun MarkdownViewer(
    markdown: String,
    modifier: Modifier = Modifier,
    textColor: Color = LocalContentColor.current,
    linkColor: Color = MaterialTheme.colorScheme.primary,
) {
    Markdown(
        content = markdown
            .replace("\\n", "\n")
            .replace("\\\'", "'"),
        // Colors fail to work just with typography
        colors = markdownColor(text = textColor),
        typography = markdownTypography(
            text = MaterialTheme.typography.bodyLarge
                .copy(color = textColor),
            textLink = TextLinkStyles(
                MaterialTheme.typography.bodyLarge
                    .copy(color = linkColor, textDecoration = TextDecoration.Underline)
                    .toSpanStyle(),
            ),
        ),
        modifier = modifier
            // Fix text color issue on iOS: https://github.com/ooni/probe-multiplatform/issues/683
            .graphicsLayer { colorFilter = ColorFilter.tint(textColor) },
        loading = { LinearProgressIndicator(modifier = modifier) },
    )
}
