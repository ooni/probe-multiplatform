package org.ooni.probe.ui.shared

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.model.MarkdownColors

@Composable
fun MarkdownViewer(
    markdown: String,
    modifier: Modifier = Modifier,
    colors: MarkdownColors = markdownColor(
        text = LocalContentColor.current,
        linkText = MaterialTheme.colorScheme.primary,
    ),
) {
    Markdown(
        content = markdown
            .replace("\\n", "\n")
            .replace("\\\'", "'"),
        colors = colors,
        modifier = modifier,
    )
}
