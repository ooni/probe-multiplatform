package org.ooni.probe.ui.shared

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        colors = @Suppress("DEPRECATION") markdownColor(
            text = textColor,
            linkText = linkColor
        ),
        typography = markdownTypography(
            text = MaterialTheme.typography.bodyLarge
                .copy(color = textColor),
            link = MaterialTheme.typography.bodyLarge
                .copy(color = MaterialTheme.colorScheme.primary),
        ),
        modifier = modifier,
    )
}
