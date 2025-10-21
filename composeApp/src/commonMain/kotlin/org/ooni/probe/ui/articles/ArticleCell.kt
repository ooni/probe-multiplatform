package org.ooni.probe.ui.articles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ooniprobe.composeapp.generated.resources.Dashboard_Articles_Blog
import ooniprobe.composeapp.generated.resources.Dashboard_Articles_Finding
import ooniprobe.composeapp.generated.resources.Dashboard_Articles_Recent
import ooniprobe.composeapp.generated.resources.Dashboard_Articles_Report
import ooniprobe.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.ArticleModel
import org.ooni.probe.data.models.ArticleModel.Source.Blog
import org.ooni.probe.data.models.ArticleModel.Source.Finding
import org.ooni.probe.data.models.ArticleModel.Source.Report
import org.ooni.probe.ui.shared.articleFormat
import org.ooni.probe.ui.theme.LocalCustomColors

@Composable
fun ArticleCell(
    article: ArticleModel,
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                article.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    stringResource(
                        when (article.source) {
                            Blog -> Res.string.Dashboard_Articles_Blog
                            Finding -> Res.string.Dashboard_Articles_Finding
                            Report -> Res.string.Dashboard_Articles_Report
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge
                        .copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Text(
                    "•",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                Text(
                    article.time.articleFormat(),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (article.isRecent) {
                    Text(
                        "•",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    Text(
                        stringResource(Res.string.Dashboard_Articles_Recent),
                        style = MaterialTheme.typography.labelLarge,
                        color = LocalCustomColors.current.success,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
