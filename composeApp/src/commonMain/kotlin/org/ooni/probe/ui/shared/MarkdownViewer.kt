package org.ooni.probe.ui.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor

@Composable
fun MarkdownViewer(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    Column {
        Markdown(
            content = markdown
                .replace("\\n", "\n")
                .replace("\\\'", "'"),
            colors = markdownColor(linkText = MaterialTheme.colorScheme.primary),
            modifier = modifier,
        )
    }
}
