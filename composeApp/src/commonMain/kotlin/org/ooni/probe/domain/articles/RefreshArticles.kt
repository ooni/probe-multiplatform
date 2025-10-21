package org.ooni.probe.domain.articles

import co.touchlab.kermit.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.ooni.engine.Engine.MkException
import org.ooni.engine.models.Failure
import org.ooni.engine.models.Result
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.config.OrganizationConfig
import org.ooni.probe.data.models.ArticleModel

class RefreshArticles(
    val httpDo: suspend (String, String, TaskOrigin) -> Result<String?, MkException>,
    val refreshArticlesInDatabase: suspend (List<ArticleModel>) -> Unit,
) {
    fun interface Source {
        suspend operator fun invoke(): Result<List<ArticleModel>, Exception>
    }

    suspend operator fun invoke() {
        if (!OrganizationConfig.hasOoniNews) return

        val sources = listOf(
            GetRSSFeed(httpDo, "https://ooni.org/blog/index.xml", ArticleModel.Source.Blog),
            GetRSSFeed(httpDo, "https://ooni.org/reports/index.xml", ArticleModel.Source.Report),
            GetFindings(httpDo),
        )

        val responses = sources
            .map {
                coroutineScope { async { it() } }
            }.awaitAll()

        responses.forEach { response ->
            response.onFailure {
                Logger.w("Failed to get article source", it)
            }
        }

        if (responses.any { it is Failure }) return

        refreshArticlesInDatabase(
            responses.mapNotNull { it.get() }.flatten(),
        )
    }
}
