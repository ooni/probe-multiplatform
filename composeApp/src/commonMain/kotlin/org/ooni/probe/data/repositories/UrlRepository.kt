package org.ooni.probe.data.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.ooni.engine.models.WebConnectivityCategory
import org.ooni.probe.Database
import org.ooni.probe.data.Url
import org.ooni.probe.data.models.UrlModel
import kotlin.coroutines.CoroutineContext

class UrlRepository(
    private val database: Database,
    private val backgroundContext: CoroutineContext,
) {
    suspend fun createOrUpdate(model: UrlModel): UrlModel.Id =
        withContext(backgroundContext) {
            database.transactionWithResult {
                createOrUpdateWithoutTransaction(model)
            }
        }

    private fun createOrUpdateWithoutTransaction(model: UrlModel): UrlModel.Id {
        database.urlQueries.insertOrReplace(
            id = model.id?.value,
            url = model.url,
            country_code = model.countryCode,
            category_code = model.category.code,
        )

        return model.id ?: UrlModel.Id(
            database.urlQueries.selectLastInsertedRowId().executeAsOne(),
        )
    }

    suspend fun createOrUpdateByUrl(models: List<UrlModel>): List<UrlModel> =
        withContext(backgroundContext) {
            database.transactionWithResult {
                val urlsWithoutId = models.filter { it.id == null }.map { it.url }
                val existingModels: List<UrlModel> =
                    // Some lists are too large for a single SQL query
                    urlsWithoutId.chunked(300) { urlsChunk ->
                        database.urlQueries.selectByUrls(urlsChunk).executeAsList()
                    }
                        .flatMap { list -> list.mapNotNull { it.toModel() } }

                models.map { model ->
                    if (model.id != null) {
                        createOrUpdateWithoutTransaction(model)
                        model
                    } else {
                        val existingModel = existingModels.firstOrNull { it.url == model.url }

                        if (existingModel != null) {
                            val modelWithId = model.copy(id = existingModel.id)
                            if (model != existingModel) {
                                createOrUpdateWithoutTransaction(modelWithId)
                            }
                            modelWithId
                        } else {
                            val modelId = createOrUpdateWithoutTransaction(model)
                            model.copy(id = modelId)
                        }
                    }
                }
            }
        }

    suspend fun getOrCreateByUrl(url: String): UrlModel =
        listByUrls(listOf(url))
            .first()
            .firstOrNull()
            ?: run {
                val newModel = UrlModel(
                    url = url,
                    category = WebConnectivityCategory.MISC,
                    countryCode = null,
                )
                newModel.copy(id = createOrUpdate(newModel))
            }

    fun list(): Flow<List<UrlModel>> =
        database.urlQueries
            .selectAll()
            .asFlow()
            .mapToList(backgroundContext)
            .map { list -> list.mapNotNull { it.toModel() } }

    private fun listByUrls(urls: List<String>): Flow<List<UrlModel>> =
        database.urlQueries
            .selectByUrls(urls)
            .asFlow()
            .mapToList(backgroundContext)
            .map { list -> list.mapNotNull { it.toModel() } }
}

fun Url.toModel(): UrlModel? {
    return UrlModel(
        id = UrlModel.Id(id),
        url = url ?: return null,
        countryCode = country_code,
        category = WebConnectivityCategory.fromCode(category_code),
    )
}
