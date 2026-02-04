package org.ooni.probe.ui.articles

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import kotlinx.datetime.LocalDate
import ooniprobe.composeapp.generated.resources.Dashboard_Articles_Blog
import ooniprobe.composeapp.generated.resources.Dashboard_Articles_Finding
import ooniprobe.composeapp.generated.resources.Dashboard_Articles_Recent
import ooniprobe.composeapp.generated.resources.Dashboard_Articles_Report
import ooniprobe.composeapp.generated.resources.Res
import ooniprobe.composeapp.generated.resources.ic_cloud_off
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ooni.probe.data.models.ArticleModel
import org.ooni.probe.shared.toDateTime
import org.ooni.probe.ui.shared.articleFormat
import org.ooni.probe.ui.theme.AppTheme
import org.ooni.probe.ui.theme.LocalCustomColors

@Composable
fun ArticleCard(
    article: ArticleModel,
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick,
        colors = CardDefaults
            .outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
    ) {
        Row(
            Modifier
                .height(IntrinsicSize.Min)
                .defaultMinSize(minHeight = 88.dp),
        ) {
            Column(Modifier.weight(2f)) {
                Text(
                    article.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(top = 8.dp, bottom = 4.dp),
                )
                Text(
                    buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            ),
                        ) {
                            append(
                                stringResource(
                                    when (article.source) {
                                        ArticleModel.Source.Blog -> Res.string.Dashboard_Articles_Blog
                                        ArticleModel.Source.Finding -> Res.string.Dashboard_Articles_Finding
                                        ArticleModel.Source.Report -> Res.string.Dashboard_Articles_Report
                                    },
                                ),
                            )
                        }
                        append(" • ")
                        append(article.time.articleFormat())
                        if (article.isRecent) {
                            append(" • ")
                            withStyle(SpanStyle(color = LocalCustomColors.current.success)) {
                                append(stringResource(Res.string.Dashboard_Articles_Recent))
                            }
                        }
                    },
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 8.dp),
                )
            }
            article.imageUrl?.let { imageUrl ->
                val painter = rememberAsyncImagePainter(imageUrl)
                val state by painter.state.collectAsStateWithLifecycle()
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxHeight()
                        .height(IntrinsicSize.Min)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            Dp.Hairline,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                            MaterialTheme.shapes.medium,
                        ).weight(1f),
                ) {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                    if (state is AsyncImagePainter.State.Loading) {
                        CircularProgressIndicator(
                            Modifier.size(48.dp),
                        )
                    }
                    if (state is AsyncImagePainter.State.Error) {
                        Image(
                            painterResource(Res.drawable.ic_cloud_off),
                            contentDescription = null,
                            modifier = Modifier.alpha(0.5f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun ArticleCellPreview() {
    AppTheme {
        ArticleCard(
            article = ArticleModel(
                url = ArticleModel.Url("http://ooni.org"),
                title = "Join us at the OMG Village at the Global Gathering 2025!",
                description = "Hello there.",
                imageUrl = "https://ooni.org/images/logos/OONI-VerticalColor@2x.png",
                source = ArticleModel.Source.Blog,
                time = LocalDate(2025, 4, 1).toDateTime(),
            ),
            onClick = {},
        )
    }
}
