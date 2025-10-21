package org.ooni.probe.data.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.ooni.probe.Database
import org.ooni.probe.data.Article
import org.ooni.probe.data.models.ArticleModel
import org.ooni.probe.shared.toEpoch
import org.ooni.probe.shared.toLocalDateTime
import kotlin.coroutines.CoroutineContext

class ArticleRepository(
    private val database: Database,
    private val backgroundContext: CoroutineContext,
) {
    suspend fun refresh(models: List<ArticleModel>) {
        withContext(backgroundContext) {
            database.transaction {
                models.forEach { model ->
                    database.articleQueries.insertOrReplace(
                        url = model.url.value,
                        title = model.title,
                        description = model.description,
                        source = model.source.value,
                        time = model.time.toEpoch(),
                    )
                }
                database.articleQueries.deleteExceptUrls(models.map { it.url.value })
            }
        }
    }

    fun list(): Flow<List<ArticleModel>> =
        database.articleQueries
            .selectAll()
            .asFlow()
            .mapToList(backgroundContext)
            .map { list -> list.mapNotNull { it.toModel() } }

    private fun Article.toModel() =
        run {
            ArticleModel(
                url = ArticleModel.Url(url),
                title = title,
                description = description,
                source = ArticleModel.Source.fromValue(source) ?: return@run null,
                time = time.toLocalDateTime(),
            )
        }
}
