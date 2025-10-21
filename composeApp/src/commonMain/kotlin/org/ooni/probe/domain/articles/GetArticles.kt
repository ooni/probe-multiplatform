package org.ooni.probe.domain.articles

import kotlinx.coroutines.flow.Flow
import org.ooni.probe.data.models.ArticleModel

class GetArticles(
    val getArticles: () -> Flow<List<ArticleModel>>,
) {
    operator fun invoke() = getArticles()
}
